package vn.edu.phenikaa.unicateen.domain;

import static org.junit.jupiter.api.Assertions.*;
import static vn.edu.phenikaa.unicateen.support.Fixtures.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentMethod;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentStatus;
import vn.edu.phenikaa.unicateen.domain.enums.PreparationStatus;
import vn.edu.phenikaa.unicateen.domain.enums.Role;
import vn.edu.phenikaa.unicateen.domain.model.Order;
import vn.edu.phenikaa.unicateen.domain.model.Payment;
import vn.edu.phenikaa.unicateen.domain.model.Preparation;
import vn.edu.phenikaa.unicateen.domain.model.User;

/** May trang thai giao dich thanh toan (TC-PAY, TC-ST): BR18-BR28. */
class PaymentStateTest {
    private final User cashier = user(3, Role.CASHIER);

    @AfterEach
    void resetClock() {
        Clock.reset();
    }

    @Test
    void openOnlyForPendingPaymentOrder_amountEqualsTotal_BR19() {
        Order draft = order("DH-1", cashier);
        draft.addItem(item(1, "Cơm"), 1, 35_000);
        assertEquals("BR19", assertThrows(BusinessRuleException.class,
                () -> Payment.open("GD-1", draft, PaymentMethod.CASH, null, null, "k1")).getRule());
        Order o = pendingOrder(cashier);
        Payment p = Payment.open("GD-1", o, PaymentMethod.CASH, null, null, "k1");
        assertEquals(PaymentStatus.PENDING, p.getStatus());
        assertEquals(70_000, p.getAmount());
    }

    @Test
    void cashNeedsEnoughMoney_andStoresChange_BR25() {
        Payment p = Payment.open("GD-1", pendingOrder(cashier), PaymentMethod.CASH, null, null, "k1");
        assertEquals("BR25", assertThrows(BusinessRuleException.class, () -> p.recordCash(69_999)).getRule());
        p.recordCash(100_000);
        assertEquals(30_000L, p.getChangeAmount());
        p.markSuccess(null, cashier);
        assertEquals(PaymentStatus.SUCCESS, p.getStatus());
        assertNotNull(p.getTransactionTime());
    }

    @Test
    void cashCannotSucceedWithoutReceivedAmount_BR25() {
        Payment p = Payment.open("GD-1", pendingOrder(cashier), PaymentMethod.CASH, null, null, "k1");
        assertThrows(BusinessRuleException.class, () -> p.markSuccess(null, cashier));
    }

    @Test
    void cardRequiresScannedCode_BR24() {
        Order o = pendingOrder(cashier);
        assertEquals("BR24", assertThrows(BusinessRuleException.class,
                () -> Payment.open("GD-1", o, PaymentMethod.STUDENT_FACULTY_CARD, null, " ", "k1")).getRule());
    }

    @Test
    void transferUsesOrderCodeAsReference_andNeedsConfirmer_BR26() {
        Payment p = Payment.open("GD-1", pendingOrder(cashier), PaymentMethod.BANK_TRANSFER, null, null, "k1");
        assertEquals("DH2611080001", p.getReferenceCode());
        assertThrows(BusinessRuleException.class, () -> p.markSuccess(null, null));
        p.markSuccess(null, cashier);
        assertSame(cashier, p.getConfirmedBy());
    }

    @Test
    void terminalStatesCannotChange_BR28() {
        Payment p = Payment.open("GD-1", pendingOrder(cashier), PaymentMethod.BANK_TRANSFER, null, null, "k1");
        p.markFailed("Không nhận được tiền", cashier);
        assertEquals(PaymentStatus.FAILED, p.getStatus());
        assertFalse(p.isActive());
        assertThrows(BusinessRuleException.class, () -> p.markSuccess(null, cashier));
        assertThrows(BusinessRuleException.class, () -> p.markFailed("lần 2", cashier));
    }

    @Test
    void failedRequiresReason_BR27() {
        Payment p = Payment.open("GD-1", pendingOrder(cashier), PaymentMethod.BANK_TRANSFER, null, null, "k1");
        assertThrows(BusinessRuleException.class, () -> p.markFailed("", cashier));
    }

    @Test
    void cardPendingExpiresAfterOneMinute_transferNeedsAttentionAfter15_BR27() {
        freezeClock(LocalDateTime.of(2026, 11, 8, 11, 0, 0));
        Payment card = Payment.open("GD-1", pendingOrder(cashier), PaymentMethod.STUDENT_FACULTY_CARD,
                student(7, "SV1"), "SV1", "k1");
        Payment tr = Payment.open("GD-2", pendingOrder(cashier), PaymentMethod.BANK_TRANSFER, null, null, "k2");
        LocalDateTime t = LocalDateTime.of(2026, 11, 8, 11, 0, 59);
        assertFalse(card.isExpired(t, 1));
        assertTrue(card.isExpired(t.plusSeconds(2), 1));
        assertFalse(tr.isExpired(t.plusHours(1), 1));
        assertFalse(tr.needsAttention(LocalDateTime.of(2026, 11, 8, 11, 14, 0), 15));
        assertTrue(tr.needsAttention(LocalDateTime.of(2026, 11, 8, 11, 15, 1), 15));
    }

    @Test
    void preparationOnlyForPaidOrder_andMovesForwardOnly_BR39() {
        Order o = pendingOrder(cashier);
        assertThrows(BusinessRuleException.class, () -> Preparation.notStarted(o));
        o.markPaid(null);
        Preparation p = Preparation.notStarted(o);
        User bep = user(6, Role.KITCHEN_STAFF);
        assertThrows(BusinessRuleException.class, () -> p.advance(PreparationStatus.READY, bep));
        p.advance(PreparationStatus.PREPARING, bep);
        p.advance(PreparationStatus.READY, bep);
        assertThrows(BusinessRuleException.class, () -> p.advance(PreparationStatus.PREPARING, bep));
        p.advance(PreparationStatus.SERVED, bep);
        assertNotNull(p.getServedAt());
        assertThrows(BusinessRuleException.class, () -> p.advance(PreparationStatus.SERVED, bep));
    }
}
