package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/** Dong phieu nhap; moi dong tao dung mot lo khi xac nhan (GD6). */
@Entity
@Table(name = "stock_receipt_details")
public class StockReceiptDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_id")
    private StockReceipt receipt;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantity;
    @Column(name = "unit_cost", nullable = false)
    private long unitCost;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Column(name = "line_total", nullable = false)
    private long lineTotal;

    protected StockReceiptDetail() {
    }

    StockReceiptDetail(StockReceipt receipt, Ingredient ingredient, BigDecimal quantity, long unitCost,
                       LocalDate expiryDate) {
        this.receipt = receipt;
        this.ingredient = ingredient;
        this.quantity = quantity;
        this.unitCost = unitCost;
        this.expiryDate = expiryDate;
        this.lineTotal = quantity.multiply(BigDecimal.valueOf(unitCost)).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    public Long getId() { return id; }
    public StockReceipt getReceipt() { return receipt; }
    public Ingredient getIngredient() { return ingredient; }
    public BigDecimal getQuantity() { return quantity; }
    public long getUnitCost() { return unitCost; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public long getLineTotal() { return lineTotal; }
}
