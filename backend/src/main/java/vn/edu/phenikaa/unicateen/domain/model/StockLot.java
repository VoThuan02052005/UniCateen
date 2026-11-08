package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;

/** Lo hang; ton cua lo khong am (BR35), xuat theo FEFO (BR34). */
@Entity
@Table(name = "stock_lots")
public class StockLot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String code;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ingredient_id")
    private Ingredient ingredient;
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receipt_detail_id")
    private StockReceiptDetail receiptDetail;
    @Column(name = "received_date", nullable = false)
    private LocalDate receivedDate;
    @Column(name = "expiry_date")
    private LocalDate expiryDate;
    @Column(name = "unit_cost", nullable = false)
    private long unitCost;
    @Column(name = "remaining_qty", nullable = false, precision = 12, scale = 3)
    private BigDecimal remainingQty;

    protected StockLot() {
    }

    /** GD6: tao tu mot dong phieu nhap da xac nhan. */
    public static StockLot fromReceiptLine(String code, StockReceiptDetail line) {
        StockLot lot = new StockLot();
        lot.code = code;
        lot.ingredient = line.getIngredient();
        lot.receiptDetail = line;
        lot.receivedDate = line.getReceipt().getReceiptDate();
        lot.expiryDate = line.getExpiryDate();
        lot.unitCost = line.getUnitCost();
        lot.remainingQty = line.getQuantity();
        return lot;
    }

    public boolean isExpired(LocalDate today) {
        return expiryDate != null && expiryDate.isBefore(today);
    }

    /** BR38: con tu {@code days} ngay tro xuong den han. */
    public boolean isNearExpiry(LocalDate today, int days) {
        return expiryDate != null && !isExpired(today) && !expiryDate.isAfter(today.plusDays(days));
    }

    /** BR34, BR35: tru ton khi xac nhan phieu xuat; khong xuat lo qua han, khong de ton am. */
    public void deduct(BigDecimal qty, LocalDate today) {
        if (isExpired(today)) {
            throw new BusinessRuleException("BR34", "Lô " + code + " đã quá hạn, không được xuất");
        }
        if (qty.signum() <= 0) {
            throw new BusinessRuleException("DV04", "Lượng xuất phải lớn hơn 0");
        }
        if (qty.compareTo(remainingQty) > 0) {
            throw new BusinessRuleException("BR35", "Lượng xuất vượt tồn của lô " + code);
        }
        remainingQty = remainingQty.subtract(qty);
    }

    /** BR37: dat ton ve so thuc te sau kiem ke; tra ve chenh lech (co dau). */
    public BigDecimal adjustTo(BigDecimal actual) {
        if (actual == null || actual.signum() < 0) {
            throw new BusinessRuleException("BR35", "Tồn sau điều chỉnh không được âm");
        }
        BigDecimal diff = actual.subtract(remainingQty);
        remainingQty = actual;
        return diff;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public Ingredient getIngredient() { return ingredient; }
    public StockReceiptDetail getReceiptDetail() { return receiptDetail; }
    public LocalDate getReceivedDate() { return receivedDate; }
    public LocalDate getExpiryDate() { return expiryDate; }
    public long getUnitCost() { return unitCost; }
    public BigDecimal getRemainingQty() { return remainingQty; }
}
