package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.domain.enums.OrderStatus;

/**
 * Don hang (UC2.1-UC2.4). May trang thai: DRAFT -> PENDING_PAYMENT -> PAID; DRAFT/PENDING_PAYMENT -> CANCELLED.
 * Viec kiem tra "khong co giao dich PENDING" do service thuc hien truoc khi goi reopen/cancel (BR13, BR15).
 */
@Entity
@Table(name = "orders")
public class Order {
    public static final int MAX_QTY = 99;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String code;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cashier_id")
    private User cashier;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;
    @Column(name = "total_amount", nullable = false)
    private long totalAmount;
    @Column(name = "cancel_reason")
    private String cancelReason;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "paid_at")
    private LocalDateTime paidAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<OrderDetail> details = new ArrayList<>();

    protected Order() {
    }

    /** BR10: don moi o DRAFT, gan nhan vien tao va thoi diem tao. */
    public Order(String code, User cashier) {
        this.code = code;
        this.cashier = cashier;
        this.status = OrderStatus.DRAFT;
        this.createdAt = Clock.now();
    }

    /** BR11: them mon hoac cong so luong neu mon da co dong; don gia lay tai thoi diem them. */
    public void addItem(MenuItem item, int quantity, long unitPrice) {
        requireStatus(OrderStatus.DRAFT, "BR13");
        item.ensureOnSale();
        Optional<OrderDetail> existing = findLine(item.getId());
        if (existing.isPresent()) {
            existing.get().changeQuantity(existing.get().getQuantity() + quantity);
        } else {
            details.add(new OrderDetail(this, item, quantity, unitPrice));
        }
        recalculate();
    }

    public void changeQuantity(Long menuItemId, int quantity) {
        requireStatus(OrderStatus.DRAFT, "BR13");
        OrderDetail line = findLine(menuItemId)
                .orElseThrow(() -> new BusinessRuleException("BR11", "Món không có trong đơn"));
        line.changeQuantity(quantity);
        recalculate();
    }

    public void removeItem(Long menuItemId) {
        requireStatus(OrderStatus.DRAFT, "BR13");
        details.removeIf(d -> d.getMenuItem().getId().equals(menuItemId));
        recalculate();
    }

    /** DRAFT -> PENDING_PAYMENT; don phai co it nhat mot dong (BR12). */
    public void submit() {
        requireStatus(OrderStatus.DRAFT, "BR13");
        if (details.isEmpty()) {
            throw new BusinessRuleException("BR12", "Đơn chưa có món");
        }
        status = OrderStatus.PENDING_PAYMENT;
    }

    /** PENDING_PAYMENT -> DRAFT de sua; service da kiem tra khong co giao dich PENDING (BR13). */
    public void reopen() {
        requireStatus(OrderStatus.PENDING_PAYMENT, "BR13");
        status = OrderStatus.DRAFT;
    }

    /** BR14, BR15: chi huy don DRAFT hoac PENDING_PAYMENT; don cho thanh toan bat buoc ly do. */
    public void cancel(String reason, User by) {
        if (status == OrderStatus.PAID) {
            throw new BusinessRuleException("BR14", "Đơn đã thanh toán không được hủy");
        }
        if (status == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("BR15", "Đơn đã hủy");
        }
        if (status == OrderStatus.PENDING_PAYMENT && (reason == null || reason.isBlank())) {
            throw new BusinessRuleException("BR15", "Hủy đơn đang chờ thanh toán phải ghi lý do");
        }
        status = OrderStatus.CANCELLED;
        cancelReason = reason;
        cancelledBy = by;
    }

    /** BR19: chi tao giao dich cho don PENDING_PAYMENT co tong tien > 0. */
    public void ensurePendingPayment() {
        requireStatus(OrderStatus.PENDING_PAYMENT, "BR19");
        if (totalAmount <= 0) {
            throw new BusinessRuleException("BR19", "Tổng tiền đơn phải lớn hơn 0");
        }
    }

    /** PENDING_PAYMENT -> PAID. Chi PaymentService goi, cung giao dich CSDL voi Payment SUCCESS (BR22). */
    public void markPaid(User payer) {
        requireStatus(OrderStatus.PENDING_PAYMENT, "BR22");
        this.customer = payer;
        this.status = OrderStatus.PAID;
        this.paidAt = Clock.now();
    }

    public boolean isOwnedBy(Long userId) {
        return cashier.getId().equals(userId);
    }

    /** Ma tham chieu chuyen khoan: ma don viet lien (BR26). */
    public String transferReference() {
        return code.replace("-", "");
    }

    private Optional<OrderDetail> findLine(Long menuItemId) {
        return details.stream().filter(d -> d.getMenuItem().getId().equals(menuItemId)).findFirst();
    }

    private void recalculate() {
        totalAmount = details.stream().mapToLong(OrderDetail::getLineTotal).sum();
    }

    private void requireStatus(OrderStatus expected, String rule) {
        if (status != expected) {
            throw new BusinessRuleException(rule,
                    "Đơn " + code + " đang ở trạng thái " + status + ", cần " + expected);
        }
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public User getCashier() { return cashier; }
    public User getCustomer() { return customer; }
    public OrderStatus getStatus() { return status; }
    public long getTotalAmount() { return totalAmount; }
    public String getCancelReason() { return cancelReason; }
    public User getCancelledBy() { return cancelledBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public List<OrderDetail> getDetails() { return details; }
}
