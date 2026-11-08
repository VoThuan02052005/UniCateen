package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.Clock;

/** Phieu dieu chinh ton mot lo (UC4.3, BR37); khong sua, khong xoa. */
@Entity
@Table(name = "stock_adjustments")
public class StockAdjustment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String code;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id")
    private StockLot lot;
    @Column(name = "qty_before", nullable = false, precision = 12, scale = 3)
    private BigDecimal qtyBefore;
    @Column(name = "qty_after", nullable = false, precision = 12, scale = 3)
    private BigDecimal qtyAfter;
    @Column(nullable = false)
    private String reason;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected StockAdjustment() {
    }

    /** Ap dung dieu chinh len lo va luu phieu; tra ve phieu chua luu. */
    public static StockAdjustment apply(String code, StockLot lot, BigDecimal actualQty, String reason, User by) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessRuleException("BR37", "Điều chỉnh tồn bắt buộc ghi lý do");
        }
        StockAdjustment a = new StockAdjustment();
        a.code = code;
        a.lot = lot;
        a.qtyBefore = lot.getRemainingQty();
        if (actualQty != null && actualQty.compareTo(a.qtyBefore) == 0) {
            throw new BusinessRuleException("BR37", "Tồn thực tế bằng tồn hệ thống, không cần điều chỉnh");
        }
        lot.adjustTo(actualQty);
        a.qtyAfter = actualQty;
        a.reason = reason.trim();
        a.createdBy = by;
        a.createdAt = Clock.now();
        return a;
    }

    public BigDecimal difference() {
        return qtyAfter.subtract(qtyBefore);
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public StockLot getLot() { return lot; }
    public BigDecimal getQtyBefore() { return qtyBefore; }
    public BigDecimal getQtyAfter() { return qtyAfter; }
    public String getReason() { return reason; }
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
