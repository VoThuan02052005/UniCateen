package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.domain.enums.RefType;
import vn.edu.phenikaa.unicateen.domain.enums.TxType;

/**
 * Giao dich kho (BR33). {@code quantity} mang dau: duong khi nhap/dieu chinh tang, am khi xuat/dieu chinh giam,
 * nen SUM(quantity) theo lo bang remaining_qty. ref_type + ref_id la tham chieu da hinh, khong co khoa ngoai.
 */
@Entity
@Table(name = "inventory_transactions")
public class InventoryTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id")
    private StockLot lot;
    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false)
    private TxType type;
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;
    @Enumerated(EnumType.STRING)
    @Column(name = "ref_type", nullable = false)
    private RefType refType;
    @Column(name = "ref_id", nullable = false)
    private Long refId;
    private String reason;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected InventoryTransaction() {
    }

    private InventoryTransaction(StockLot lot, TxType type, BigDecimal quantity, RefType refType, Long refId,
                                 String reason, User by) {
        this.lot = lot;
        this.type = type;
        this.quantity = quantity;
        this.refType = refType;
        this.refId = refId;
        this.reason = reason;
        this.createdBy = by;
        this.createdAt = Clock.now();
    }

    public static InventoryTransaction in(StockLot lot, BigDecimal qty, Long receiptId, User by) {
        return new InventoryTransaction(lot, TxType.IN, qty.abs(), RefType.RECEIPT, receiptId, null, by);
    }

    public static InventoryTransaction out(StockLot lot, BigDecimal qty, Long issueId, User by) {
        return new InventoryTransaction(lot, TxType.OUT, qty.abs().negate(), RefType.ISSUE, issueId, null, by);
    }

    public static InventoryTransaction adjust(StockAdjustment adj, User by) {
        return new InventoryTransaction(adj.getLot(), TxType.ADJUST, adj.difference(), RefType.ADJUSTMENT,
                adj.getId(), adj.getReason(), by);
    }

    public Long getId() { return id; }
    public StockLot getLot() { return lot; }
    public TxType getType() { return type; }
    public BigDecimal getQuantity() { return quantity; }
    public RefType getRefType() { return refType; }
    public Long getRefId() { return refId; }
    public String getReason() { return reason; }
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
