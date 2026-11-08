package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.domain.enums.OrderStatus;
import vn.edu.phenikaa.unicateen.domain.enums.PreparationStatus;

/** Trang thai chuan bi mon theo don (UC4.5, GD8, BR39). */
@Entity
@Table(name = "preparations")
public class Preparation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PreparationStatus status;
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    @Column(name = "ready_at")
    private LocalDateTime readyAt;
    @Column(name = "served_at")
    private LocalDateTime servedAt;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    protected Preparation() {
    }

    /** Tao tu dong khi don chuyen PAID (BR22). */
    public static Preparation notStarted(Order order) {
        if (order.getStatus() != OrderStatus.PAID) {
            throw new BusinessRuleException("BR39", "Chỉ tạo trạng thái chuẩn bị cho đơn đã thanh toán");
        }
        Preparation p = new Preparation();
        p.order = order;
        p.status = PreparationStatus.NOT_STARTED;
        return p;
    }

    /** BR39: chuyen sang trang thai ke tiep, khong lui, khong nhay buoc. */
    public void advance(PreparationStatus target, User by) {
        PreparationStatus next = status.next();
        if (next == null || next != target) {
            throw new BusinessRuleException("BR39",
                    "Không chuyển được từ " + status + " sang " + target);
        }
        LocalDateTime now = Clock.now();
        switch (target) {
            case PREPARING -> startedAt = now;
            case READY -> readyAt = now;
            case SERVED -> servedAt = now;
            default -> { }
        }
        status = target;
        updatedBy = by;
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public PreparationStatus getStatus() { return status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getReadyAt() { return readyAt; }
    public LocalDateTime getServedAt() { return servedAt; }
    public User getUpdatedBy() { return updatedBy; }
}
