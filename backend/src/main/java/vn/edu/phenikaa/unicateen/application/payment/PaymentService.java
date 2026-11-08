package vn.edu.phenikaa.unicateen.application.payment;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import vn.edu.phenikaa.unicateen.application.common.Actors;
import vn.edu.phenikaa.unicateen.application.common.CodeGenerator;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;
import vn.edu.phenikaa.unicateen.application.common.ExternalServiceException;
import vn.edu.phenikaa.unicateen.application.common.ForbiddenException;
import vn.edu.phenikaa.unicateen.application.common.NotFoundException;
import vn.edu.phenikaa.unicateen.application.port.IAuditLogger;
import vn.edu.phenikaa.unicateen.application.port.ICardPaymentGateway;
import vn.edu.phenikaa.unicateen.application.port.ICardPaymentGateway.CardGatewayException;
import vn.edu.phenikaa.unicateen.application.port.ICardPaymentGateway.CardResult;
import vn.edu.phenikaa.unicateen.application.port.OrderRepository;
import vn.edu.phenikaa.unicateen.application.port.PaymentRepository;
import vn.edu.phenikaa.unicateen.application.port.PreparationRepository;
import vn.edu.phenikaa.unicateen.application.port.UserRepository;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentMethod;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentStatus;
import vn.edu.phenikaa.unicateen.domain.enums.Role;
import vn.edu.phenikaa.unicateen.domain.model.Order;
import vn.edu.phenikaa.unicateen.domain.model.Payment;
import vn.edu.phenikaa.unicateen.domain.model.Preparation;
import vn.edu.phenikaa.unicateen.domain.model.User;

/**
 * Nhom 3: thanh toan (UC3.1-UC3.5) theo mo hinh Payment Transaction + Payment State Machine +
 * Order State Management.
 * <ul>
 *   <li>BR20: khoa ghi tren don truoc khi mo giao dich; UNIQUE active_order_id la lop bao ve cuoi.</li>
 *   <li>BR22: Payment SUCCESS, Order PAID va Preparation NOT_STARTED trong cung mot giao dich CSDL.</li>
 *   <li>NFR06: idempotency_key; gui lai cung khoa tra ve giao dich cu.</li>
 * </ul>
 */
@Service
public class PaymentService {
    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    public static final String CARD_TIMEOUT_REASON = "Hết thời gian chờ";

    private final PaymentRepository payments;
    private final OrderRepository orders;
    private final PreparationRepository preparations;
    private final UserRepository users;
    private final ICardPaymentGateway cardGateway;
    private final CodeGenerator codes;
    private final Actors actors;
    private final IAuditLogger audit;
    private final TransactionTemplate tx;
    private final int cardExpiryMinutes;
    private final int transferAttentionMinutes;

    public PaymentService(PaymentRepository payments, OrderRepository orders, PreparationRepository preparations,
                          UserRepository users, ICardPaymentGateway cardGateway, CodeGenerator codes, Actors actors,
                          IAuditLogger audit, PlatformTransactionManager txManager,
                          @Value("${unicateen.card.pending-expiry-minutes:1}") int cardExpiryMinutes,
                          @Value("${unicateen.transfer.needs-attention-minutes:15}") int transferAttentionMinutes) {
        this.payments = payments;
        this.orders = orders;
        this.preparations = preparations;
        this.users = users;
        this.cardGateway = cardGateway;
        this.codes = codes;
        this.actors = actors;
        this.audit = audit;
        this.tx = new TransactionTemplate(txManager);
        this.cardExpiryMinutes = cardExpiryMinutes;
        this.transferAttentionMinutes = transferAttentionMinutes;
    }

    // ------------------------------------------------------------ UC3.2 tien mat

    /** UC3.2: tao giao dich va chuyen SUCCESS trong cung giao dich CSDL; quet the tuy chon (BR23, BR25). */
    @Transactional
    public PaymentView payCash(String orderCode, long received, String cardCode, String idemKey, CurrentUser actor) {
        var existing = payments.findByIdempotencyKey(idemKey);
        if (existing.isPresent()) {
            return view(existing.get());                                       // NFR06
        }
        Order order = lockOrderForPayment(orderCode, actor);
        User payer = optionalPayer(cardCode);
        User cashier = actors.load(actor);
        Payment p = Payment.open(nextCode(), order, PaymentMethod.CASH, payer, normalize(cardCode), idemKey);
        p.recordCash(received);                                                // BR25
        p.markSuccess(null, cashier);
        completeOrder(order, payer);                                           // BR22
        return view(payments.save(p));
    }

    // ------------------------------------------------------------ UC3.1 the

    /**
     * UC3.1: (1) mo giao dich PENDING va commit; (2) goi dich vu the ngoai giao dich CSDL, cho toi da 10 giay;
     * (3) ghi ket qua trong giao dich moi. Neu may chu dung giua chung, giao dich PENDING se bi tac vu
     * expireStaleCardPayments chuyen FAILED sau 1 phut (BR27).
     */
    public PaymentView payCard(String orderCode, String cardCode, String idemKey, CurrentUser actor) {
        String card = normalize(cardCode);
        if (card == null) {
            throw new BusinessRuleException("BR24", "Thanh toán thẻ yêu cầu quét được mã thẻ");
        }
        Opened opened = tx.execute(s -> openCardPayment(orderCode, card, idemKey, actor));
        if (!opened.isNew()) {
            return opened.view();                                              // NFR06: yeu cau gui lai
        }
        CardResult result;
        try {
            result = cardGateway.charge(card, opened.view().amount(), orderCode, idemKey);
        } catch (CardGatewayException e) {
            log.warn("Dich vu the loi cho giao dich {}: {}", opened.view().code(), e.getMessage());
            tx.executeWithoutResult(s -> failCard(opened.view().code(), e.getMessage(), actor));
            throw new ExternalServiceException(e.getMessage());
        }
        return tx.execute(s -> finishCard(opened.view().code(), result, actor));
    }

    private record Opened(PaymentView view, boolean isNew) {
    }

    private Opened openCardPayment(String orderCode, String card, String idemKey, CurrentUser actor) {
        var existing = payments.findByIdempotencyKey(idemKey);
        if (existing.isPresent()) {
            return new Opened(view(existing.get()), false);
        }
        Order order = lockOrderForPayment(orderCode, actor);
        User payer = users.findByCardCode(card)
                .orElseThrow(() -> new BusinessRuleException("BR23", "Mã thẻ không liên kết với tài khoản nào"));
        if (!payer.isActive() || !payer.getRole().isCardHolder()) {                // BR23
            throw new BusinessRuleException("BR23", "Thẻ không thuộc sinh viên, giảng viên đang hoạt động");
        }
        Payment p = Payment.open(nextCode(), order, PaymentMethod.STUDENT_FACULTY_CARD, payer, card, idemKey);
        return new Opened(view(payments.saveAndFlush(p)), true);
    }

    private PaymentView finishCard(String paymentCode, CardResult result, CurrentUser actor) {
        Order order = lockOrderOf(paymentCode);
        Payment p = lockPayment(paymentCode);
        if (p.getStatus() != PaymentStatus.PENDING) {
            return view(p);       // da bi tac vu het han xu ly truoc; giu nguyen (BR27)
        }
        User cashier = actors.load(actor);
        if (result.approved()) {
            p.markSuccess(result.reference(), cashier);
            completeOrder(order, p.getPayer());                                // BR22
        } else {
            p.markFailed(result.message() == null ? "Thẻ bị từ chối" : result.message(), cashier);
        }
        return view(p);
    }

    private void failCard(String paymentCode, String reason, CurrentUser actor) {
        Payment p = lockPayment(paymentCode);
        if (p.getStatus() == PaymentStatus.PENDING) {
            p.markFailed(reason, actors.load(actor));
        }
    }

    /** BR27: giao dich the PENDING qua 1 phut tu chuyen FAILED; don giu PENDING_PAYMENT. */
    @Scheduled(fixedDelayString = "PT30S")
    @Transactional
    public int expireStaleCardPayments() {
        LocalDateTime limit = Clock.now().minusMinutes(cardExpiryMinutes);
        List<Payment> stale = payments.findPendingCardCreatedBeforeForUpdate(limit);
        for (Payment p : stale) {
            p.markFailed(CARD_TIMEOUT_REASON, null);
            log.info("Giao dich the {} het thoi gian cho, chuyen FAILED", p.getCode());
        }
        return stale.size();
    }

    // ------------------------------------------------------------ UC3.3, UC3.4 chuyen khoan

    /** UC3.3: tao giao dich PENDING co ma tham chieu la ma don viet lien (BR26). */
    @Transactional
    public PaymentView startTransfer(String orderCode, String cardCode, String idemKey, CurrentUser actor) {
        var existing = payments.findByIdempotencyKey(idemKey);
        if (existing.isPresent()) {
            return view(existing.get());
        }
        Order order = lockOrderForPayment(orderCode, actor);
        User payer = optionalPayer(cardCode);
        Payment p = Payment.open(nextCode(), order, PaymentMethod.BANK_TRANSFER, payer, normalize(cardCode), idemKey);
        return view(payments.save(p));
    }

    /** UC3.4: nhan vien xac nhan da nhan dung tien va noi dung; luu nguoi xac nhan, ghi nhat ky (BR26). */
    @Transactional
    public PaymentView confirmTransfer(String paymentCode, CurrentUser actor) {
        Order order = lockOrderOf(paymentCode);
        Payment p = lockPayment(paymentCode);
        requireTransfer(p);
        User by = actors.load(actor);
        p.markSuccess(null, by);
        completeOrder(order, p.getPayer());                                    // BR22
        audit.log(by, "TRANSFER_CONFIRM", "payments", p.getId(), Map.of("status", "PENDING"),
                Map.of("status", "SUCCESS", "order", order.getCode()));
        return view(p);
    }

    /** UC3.4: khong nhan duoc tien hoac khach doi hinh thuc: chuyen FAILED kem ly do, ghi nhat ky (BR21, BR27). */
    @Transactional
    public PaymentView failTransfer(String paymentCode, String reason, CurrentUser actor) {
        Payment p = lockPayment(paymentCode);
        requireTransfer(p);
        User by = actors.load(actor);
        p.markFailed(reason, by);
        audit.log(by, "TRANSFER_FAIL", "payments", p.getId(), Map.of("status", "PENDING"),
                Map.of("status", "FAILED", "reason", reason));
        return view(p);
    }

    /** Hang doi chuyen khoan dang cho xac nhan. */
    @Transactional(readOnly = true)
    public List<PaymentView> pendingTransfers() {
        return payments.findByMethodAndStatusOrderByCreatedAtAsc(PaymentMethod.BANK_TRANSFER, PaymentStatus.PENDING)
                .stream().map(this::view).toList();
    }

    // ------------------------------------------------------------ UC3.5 tra cuu

    @Transactional(readOnly = true)
    public List<PaymentView> search(LocalDate from, LocalDate to, PaymentMethod method, PaymentStatus status) {
        return payments.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        from.atStartOfDay(), to.plusDays(1).atStartOfDay()).stream()
                .filter(p -> method == null || p.getMethod() == method)
                .filter(p -> status == null || p.getStatus() == status)
                .map(this::view).toList();
    }

    @Transactional(readOnly = true)
    public List<PaymentView> byOrder(String orderCode, CurrentUser actor) {
        Order o = orders.findByCode(orderCode).orElseThrow(() -> new NotFoundException("đơn", orderCode));
        checkAccess(o, actor);
        return payments.findByOrderIdOrderByCreatedAtDesc(o.getId()).stream().map(this::view).toList();
    }

    // ------------------------------------------------------------ noi bo

    /** Khoa don, kiem tra trang thai don (BR19) va giao dich dang hoat dong (BR20). */
    private Order lockOrderForPayment(String orderCode, CurrentUser actor) {
        Order order = orders.findByCodeForUpdate(orderCode)
                .orElseThrow(() -> new NotFoundException("đơn", orderCode));
        checkAccess(order, actor);
        order.ensurePendingPayment();
        if (payments.existsByOrderIdAndStatusIn(order.getId(), EnumSet.of(PaymentStatus.PENDING,
                PaymentStatus.SUCCESS))) {
            throw new BusinessRuleException("BR20", "Đơn đang có giao dịch thanh toán chờ xử lý hoặc đã thanh toán");
        }
        return order;
    }

    /** BR22: chuyen don PAID va tao trang thai chuan bi trong cung giao dich. */
    private void completeOrder(Order order, User payer) {
        order.markPaid(payer);
        preparations.save(Preparation.notStarted(order));
    }

    /** BR23: tien mat, chuyen khoan co the quet the de gan nguoi mua; ma khong hop le thi bao loi de quet lai. */
    private User optionalPayer(String cardCode) {
        String card = normalize(cardCode);
        if (card == null) {
            return null;
        }
        User u = users.findByCardCode(card)
                .orElseThrow(() -> new BusinessRuleException("BR23", "Mã thẻ không liên kết với tài khoản nào"));
        if (!u.isActive()) {
            throw new BusinessRuleException("BR23", "Tài khoản của thẻ đang bị khóa");
        }
        return u;
    }

    /** Thu tu khoa thong nhat: don truoc, giao dich sau, tranh khoa cheo. */
    private Order lockOrderOf(String paymentCode) {
        String orderCode = payments.findOrderCodeByPaymentCode(paymentCode);   // truy van vo huong, chua nap entity
        if (orderCode == null) {
            throw new NotFoundException("giao dịch", paymentCode);
        }
        return orders.findByCodeForUpdate(orderCode).orElseThrow();
    }

    private Payment lockPayment(String code) {
        return payments.findByCodeForUpdate(code).orElseThrow(() -> new NotFoundException("giao dịch", code));
    }

    private void requireTransfer(Payment p) {
        if (p.getMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new BusinessRuleException("BR26", "Chỉ xác nhận được giao dịch chuyển khoản");
        }
    }

    private void checkAccess(Order o, CurrentUser actor) {
        if (!(actor.is(Role.MANAGER) || (actor.is(Role.CASHIER) && o.isOwnedBy(actor.id())))) {
            throw new ForbiddenException("Không có quyền thanh toán đơn này");
        }
    }

    private String nextCode() {
        return codes.next("GD", 4, payments::findMaxCodeWithPrefix);
    }

    private static String normalize(String cardCode) {
        return cardCode == null || cardCode.isBlank() ? null : cardCode.trim();
    }

    private PaymentView view(Payment p) {
        return PaymentView.of(p, p.needsAttention(Clock.now(), transferAttentionMinutes));
    }
}
