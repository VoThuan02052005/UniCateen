package vn.edu.phenikaa.unicateen.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import vn.edu.phenikaa.unicateen.application.common.ExternalServiceException;
import vn.edu.phenikaa.unicateen.application.payment.PaymentService;
import vn.edu.phenikaa.unicateen.application.payment.PaymentView;
import vn.edu.phenikaa.unicateen.application.sales.OrderService;
import vn.edu.phenikaa.unicateen.application.sales.OrderService.LineInput;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentStatus;

/** TC-PAY: mo hinh Payment Transaction + State Machine + Order State Management tren MySQL that. */
class PaymentIntegrationTest extends IntegrationTest {
    @Autowired
    OrderService orders;
    @Autowired
    PaymentService payments;

    /** Don PENDING_PAYMENT 2 x Com ga (70.000 d) do CASHIER tao. */
    private String pendingOrder() {
        String code = orders.create(List.of(new LineInput(1L, 2)), CASHIER).code();
        orders.submit(code, CASHIER);
        return code;
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }

    @Test
    void cashSuccess_paysOrder_createsPreparation_inOneTransaction_BR22() {
        String code = pendingOrder();
        PaymentView p = payments.payCash(code, 100_000, null, key(), CASHIER);
        assertEquals(PaymentStatus.SUCCESS, p.status());
        assertEquals(30_000L, p.changeAmount());
        assertEquals("PAID", orderStatus(code));
        assertEquals(1, count("SELECT COUNT(*) FROM preparations pr JOIN orders o ON o.id = pr.order_id "
                + "WHERE o.code = ? AND pr.status = 'NOT_STARTED'", code));
    }

    @Test
    void cashWithScannedCard_linksBuyer_BR23() {
        String code = pendingOrder();
        payments.payCash(code, 70_000, "SV23010427", key(), CASHIER);
        assertEquals(7L, jdbc.queryForObject("SELECT customer_id FROM orders WHERE code = ?", Long.class, code));
    }

    @Test
    void sameIdempotencyKeyReturnsSamePayment_NFR06() {
        String code = pendingOrder();
        String k = key();
        PaymentView first = payments.payCash(code, 70_000, null, k, CASHIER);
        PaymentView again = payments.payCash(code, 70_000, null, k, CASHIER);
        assertEquals(first.code(), again.code());
        assertEquals(1, count("SELECT COUNT(*) FROM payments p JOIN orders o ON o.id = p.order_id WHERE o.code = ?",
                code));
    }

    @Test
    void secondPaymentWhileTransferPending_isRejected_BR20() {
        String code = pendingOrder();
        payments.startTransfer(code, null, key(), CASHIER);
        BusinessRuleException e = assertThrows(BusinessRuleException.class,
                () -> payments.payCash(code, 70_000, null, key(), CASHIER));
        assertEquals("BR20", e.getRule());
    }

    @Test
    void concurrentPayments_onlyOneSucceeds_BR20() throws Exception {
        String code = pendingOrder();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        Callable<Boolean> pay = () -> {
            start.await();
            try {
                payments.payCash(code, 70_000, null, key(), CASHIER);
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        };
        Future<Boolean> a = pool.submit(pay);
        Future<Boolean> b = pool.submit(pay);
        start.countDown();
        int ok = (a.get() ? 1 : 0) + (b.get() ? 1 : 0);
        pool.shutdown();
        assertEquals(1, ok);
        assertEquals(1, count("SELECT COUNT(*) FROM payments p JOIN orders o ON o.id = p.order_id "
                + "WHERE o.code = ? AND p.payment_status = 'SUCCESS'", code));
    }

    @Test
    void databaseRejectsTwoActivePaymentsForOneOrder_uqPayActive_BR20() {
        String code = pendingOrder();
        Long orderId = jdbc.queryForObject("SELECT id FROM orders WHERE code = ?", Long.class, code);
        String sql = "INSERT INTO payments (code, order_id, payment_method, amount, payment_status, idempotency_key, "
                + "created_at) VALUES (?, ?, 'BANK_TRANSFER', 70000, 'PENDING', ?, NOW())";
        jdbc.update(sql, "GD-T-1", orderId, key());
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(sql, "GD-T-2", orderId, key()));
        // giao dich FAILED khong chiem cho: thu lai duoc (BR21)
        jdbc.update("UPDATE payments SET payment_status = 'FAILED', failure_reason = 'test' WHERE code = 'GD-T-1'");
        jdbc.update(sql, "GD-T-3", orderId, key());
    }

    @Test
    void rollbackWhenPreparationCannotBeCreated_BR22() {
        String code = pendingOrder();
        // chen truoc mot preparation cho don (vi pham UNIQUE uq_prep_order khi thanh toan tao preparation)
        jdbc.update("INSERT INTO preparations (order_id, status) SELECT id, 'NOT_STARTED' FROM orders WHERE code = ?",
                code);
        assertThrows(RuntimeException.class, () -> payments.payCash(code, 70_000, null, key(), CASHIER));
        assertEquals("PENDING_PAYMENT", orderStatus(code));
        assertEquals(0, count("SELECT COUNT(*) FROM payments p JOIN orders o ON o.id = p.order_id WHERE o.code = ?",
                code));
    }

    @Test
    void cardApproved_paysOrderAndLinksCardHolder_UC31() {
        String code = pendingOrder();
        PaymentView p = payments.payCard(code, "SV23010320", key(), CASHIER);
        assertEquals(PaymentStatus.SUCCESS, p.status());
        assertNotNull(p.referenceCode());
        assertEquals("PAID", orderStatus(code));
    }

    @Test
    void cardDeclined_failsAndOrderStaysPending_BR21() {
        String code = pendingOrder();
        PaymentView p = payments.payCard(code, "SV23010062DECLINE", key(), CASHIER);
        assertEquals(PaymentStatus.FAILED, p.status());
        assertEquals("PENDING_PAYMENT", orderStatus(code));
        payments.payCash(code, 70_000, null, key(), CASHIER);                 // thu lai bang hinh thuc khac
        assertEquals("PAID", orderStatus(code));
    }

    @Test
    void unknownCardOrNonCardHolderIsRejected_BR23() {
        String code = pendingOrder();
        assertEquals("BR23", assertThrows(BusinessRuleException.class,
                () -> payments.payCard(code, "KHONG-CO", key(), CASHIER)).getRule());
    }

    @Test
    void cardPendingOlderThanOneMinute_isExpiredByJob_BR27() {
        String code = pendingOrder();
        Long orderId = jdbc.queryForObject("SELECT id FROM orders WHERE code = ?", Long.class, code);
        jdbc.update("INSERT INTO payments (code, order_id, payment_method, amount, payment_status, idempotency_key, "
                + "card_code, payer_id, created_at) VALUES ('GD-T-CARD', ?, 'STUDENT_FACULTY_CARD', 70000, 'PENDING', ?, "
                + "'SV23010427', 7, NOW() - INTERVAL 2 MINUTE)", orderId, key());
        assertTrue(payments.expireStaleCardPayments() >= 1);
        assertEquals("FAILED", jdbc.queryForObject("SELECT payment_status FROM payments WHERE code = 'GD-T-CARD'",
                String.class));
        assertEquals(PaymentService.CARD_TIMEOUT_REASON, jdbc.queryForObject(
                "SELECT failure_reason FROM payments WHERE code = 'GD-T-CARD'", String.class));
        assertEquals("PENDING_PAYMENT", orderStatus(code));
    }

    @Test
    void cardGatewayTimeout_marksFailed_andReturns502_BR27() {
        // the "...SLOW" phan hoi cham hon timeout (1 giay trong application-test.yml)
        jdbc.update("UPDATE users SET card_code = 'GV0001SLOW' WHERE id = 9");
        String code = pendingOrder();
        assertThrows(ExternalServiceException.class, () -> payments.payCard(code, "GV0001SLOW", key(), CASHIER));
        assertEquals(1, count("SELECT COUNT(*) FROM payments p JOIN orders o ON o.id = p.order_id "
                + "WHERE o.code = ? AND p.payment_status = 'FAILED'", code));
        jdbc.update("UPDATE users SET card_code = 'GV0001' WHERE id = 9");
    }

    @Test
    void transferConfirm_storesConfirmer_andAudits_BR26() {
        String code = pendingOrder();
        PaymentView p = payments.startTransfer(code, null, key(), CASHIER);
        assertEquals(code.replace("-", ""), p.referenceCode());
        PaymentView done = payments.confirmTransfer(p.code(), MANAGER);
        assertEquals(PaymentStatus.SUCCESS, done.status());
        assertEquals(1, count("SELECT COUNT(*) FROM audit_logs WHERE action = 'TRANSFER_CONFIRM' AND entity_id = ?",
                done.id()));
    }

    @Test
    void transferFail_needsReason_keepsOrderPending_BR21() {
        String code = pendingOrder();
        PaymentView p = payments.startTransfer(code, null, key(), CASHIER);
        PaymentView failed = payments.failTransfer(p.code(), "Không nhận được tiền", CASHIER);
        assertEquals(PaymentStatus.FAILED, failed.status());
        assertEquals("PENDING_PAYMENT", orderStatus(code));
    }

    @Test
    void cannotCancelOrReopenWhilePaymentPending_BR13_BR15() {
        String code = pendingOrder();
        payments.startTransfer(code, null, key(), CASHIER);
        assertEquals("BR15", assertThrows(BusinessRuleException.class,
                () -> orders.cancel(code, "đổi ý", CASHIER)).getRule());
        assertEquals("BR13", assertThrows(BusinessRuleException.class, () -> orders.reopen(code, CASHIER)).getRule());
    }
}
