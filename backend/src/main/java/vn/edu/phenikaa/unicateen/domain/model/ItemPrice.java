package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.enums.PriceStatus;

/** Lich su gia ban (UC1.3). Doi gia tao ban ghi moi, khong sua de (BR05). */
@Entity
@Table(name = "item_prices")
public class ItemPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;
    @Column(nullable = false)
    private long price;
    @Column(name = "effective_from", nullable = false)
    private LocalDateTime effectiveFrom;
    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;
    private String reason;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriceStatus status;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;

    protected ItemPrice() {
    }

    public ItemPrice(MenuItem item, long price, LocalDateTime from, String reason, User by, LocalDateTime now) {
        if (price <= 0) {
            throw new BusinessRuleException("BR02", "Giá bán phải là số nguyên dương");
        }
        this.menuItem = item;
        this.price = price;
        this.effectiveFrom = from;
        this.reason = reason;
        this.createdBy = by;
        this.status = from.isAfter(now) ? PriceStatus.SCHEDULED : PriceStatus.ACTIVE;
    }

    public boolean isEffectiveAt(LocalDateTime t) {
        return status != PriceStatus.CANCELLED && !effectiveFrom.isAfter(t)
                && (effectiveTo == null || effectiveTo.isAfter(t));
    }

    /** Ket thuc hieu luc khi co gia moi. */
    public void closeAt(LocalDateTime t) {
        if (effectiveTo == null || effectiveTo.isAfter(t)) {
            effectiveTo = t;
        }
    }

    public void activate() {
        if (status == PriceStatus.SCHEDULED) {
            status = PriceStatus.ACTIVE;
        }
    }

    /** Gia bat dau tu hoac sau moc cua gia moi thi bi thay the hoan toan. */
    public void supersede() {
        status = PriceStatus.CANCELLED;
    }

    public void cancel() {
        if (status != PriceStatus.SCHEDULED) {
            throw new BusinessRuleException("BR05", "Chỉ hủy được giá chưa đến thời điểm hiệu lực");
        }
        status = PriceStatus.CANCELLED;
    }

    public Long getId() { return id; }
    public MenuItem getMenuItem() { return menuItem; }
    public long getPrice() { return price; }
    public LocalDateTime getEffectiveFrom() { return effectiveFrom; }
    public LocalDateTime getEffectiveTo() { return effectiveTo; }
    public String getReason() { return reason; }
    public PriceStatus getStatus() { return status; }
    public User getCreatedBy() { return createdBy; }
}
