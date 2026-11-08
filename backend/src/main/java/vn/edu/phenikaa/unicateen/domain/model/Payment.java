package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentMethod;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentStatus;

/**
 * Giao dich thanh toan (UC3.1-UC3.5). May trang thai: PENDING -> SUCCESS | FAILED.
 * Toi da mot giao dich PENDING/SUCCESS moi don, bao dam bang cot sinh active_order_id UNIQUE (BR20).
 * Giao dich khong bi xoa (BR28).
 */
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String code;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false)
    private PaymentMethod method;
    @Column(nullable = false)
    private long amount;
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    private PaymentStatus status;
    @Column(name = "reference_code", length = 50)
    private String referenceCode;
    @Column(name = "received_amount")
    private Long receivedAmount;
    @Column(name = "change_amount")
    private Long changeAmount;
    @Column(name = "failure_reason")
    private String failureReason;
    @Column(name = "idempotency_key", nullable = false, length = 64)
    private String idempotencyKey;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payer_id")
    private User payer;
    @Column(name = "card_code", length = 20)
    private String cardCode;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "confirmed_by")
    private User confirmedBy;
    @Column(name = "transaction_time")
    private LocalDateTime transactionTime;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Payment() {
    }

    /** Mo giao dich PENDING cho don dang cho thanh toan; so tien bang tong tien don (BR19). */
    public static Payment open(String code, Order order, PaymentMethod method, User payer, String cardCode,
                               String idempotencyKey) {
        order.ensurePendingPayment();
        if (method == PaymentMethod.STUDENT_FACULTY_CARD && (cardCode == null || cardCode.isBlank())) {
            throw new BusinessRuleException("BR24", "Thanh toán thẻ yêu cầu quét được mã thẻ");
        }
        Payment p = new Payment();
        p.code = code;
        p.order = order;
        p.method = method;
        p.amount = order.getTotalAmount();
        p.status = PaymentStatus.PENDING;
        p.payer = payer;
        p.cardCode = cardCode;
        p.idempotencyKey = idempotencyKey;
        p.createdAt = Clock.now();
        if (method == PaymentMethod.BANK_TRANSFER) {
            p.referenceCode = order.transferReference();          // BR26
        }
        return p;
    }

    /** BR25: tien nhan >= tong tien; luu tien nhan va tien thua. */
    public void recordCash(long received) {
        requireMethod(PaymentMethod.CASH);
        requirePending();
        if (received < amount) {
            throw new BusinessRuleException("BR25", "Số tiền nhận nhỏ hơn tổng tiền đơn");
        }
        receivedAmount = received;
        changeAmount = received - amount;
    }

    /**
     * PENDING -> SUCCESS. Service phai goi Order.markPaid va tao Preparation trong cung giao dich (BR22).
     * Voi chuyen khoan, {@code by} la nguoi xac nhan (BR26).
     */
    public void markSuccess(String reference, User by) {
        requirePending();
        if (method == PaymentMethod.CASH && receivedAmount == null) {
            throw new BusinessRuleException("BR25", "Chưa ghi nhận số tiền nhận");
        }
        if (method == PaymentMethod.BANK_TRANSFER && by == null) {
            throw new BusinessRuleException("BR26", "Chuyển khoản phải có người xác nhận");
        }
        if (reference != null) {
            referenceCode = reference;
        }
        status = PaymentStatus.SUCCESS;
        confirmedBy = by;
        transactionTime = Clock.now();
    }

    /** PENDING -> FAILED; giao dich luu lam lich su, don giu PENDING_PAYMENT (BR21). */
    public void markFailed(String reason, User by) {
        requirePending();
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("BR27", "Phải ghi lý do khi chuyển giao dịch sang thất bại");
        }
        status = PaymentStatus.FAILED;
        failureReason = reason;
        confirmedBy = by;
        transactionTime = Clock.now();
    }

    /** BR27: giao dich the PENDING qua han. */
    public boolean isExpired(LocalDateTime now, int minutes) {
        return status == PaymentStatus.PENDING && method == PaymentMethod.STUDENT_FACULTY_CARD
                && createdAt.plusMinutes(minutes).isBefore(now);
    }

    /** BR27: chuyen khoan PENDING qua 15 phut can xu ly. */
    public boolean needsAttention(LocalDateTime now, int minutes) {
        return status == PaymentStatus.PENDING && method == PaymentMethod.BANK_TRANSFER
                && createdAt.plusMinutes(minutes).isBefore(now);
    }

    public boolean isActive() {
        return status.isActive();
    }

    private void requirePending() {
        if (status != PaymentStatus.PENDING) {
            throw new BusinessRuleException("BR28", "Giao dịch " + code + " không còn ở trạng thái PENDING");
        }
    }

    private void requireMethod(PaymentMethod m) {
        if (method != m) {
            throw new BusinessRuleException("BR18", "Sai hình thức thanh toán");
        }
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public Order getOrder() { return order; }
    public PaymentMethod getMethod() { return method; }
    public long getAmount() { return amount; }
    public PaymentStatus getStatus() { return status; }
    public String getReferenceCode() { return referenceCode; }
    public Long getReceivedAmount() { return receivedAmount; }
    public Long getChangeAmount() { return changeAmount; }
    public String getFailureReason() { return failureReason; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public User getPayer() { return payer; }
    public String getCardCode() { return cardCode; }
    public User getConfirmedBy() { return confirmedBy; }
    public LocalDateTime getTransactionTime() { return transactionTime; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
