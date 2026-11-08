package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.enums.ActiveStatus;

/** Nguyen lieu (UC1.4). Don vi tinh co dinh khi da co giao dich kho (BR07). */
@Entity
@Table(name = "ingredients")
public class Ingredient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 10)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "group_name", nullable = false, length = 50)
    private String groupName;
    @Column(nullable = false, length = 10)
    private String unit;
    @Column(name = "min_stock", nullable = false, precision = 12, scale = 3)
    private BigDecimal minStock;
    @Column(name = "track_expiry", nullable = false)
    private boolean trackExpiry;
    @Column(name = "storage_note")
    private String storageNote;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActiveStatus status;

    protected Ingredient() {
    }

    public Ingredient(String code, String name, String groupName, String unit, BigDecimal minStock,
                      boolean trackExpiry, String storageNote) {
        this.code = code;
        this.unit = unit.trim();
        this.status = ActiveStatus.ACTIVE;
        update(name, groupName, minStock, trackExpiry, storageNote);
    }

    public void update(String name, String groupName, BigDecimal minStock, boolean trackExpiry, String storageNote) {
        if (minStock.signum() < 0) {
            throw new BusinessRuleException("DV04", "Mức tồn tối thiểu không được âm");
        }
        this.name = name.trim();
        this.groupName = groupName;
        this.minStock = minStock;
        this.trackExpiry = trackExpiry;
        this.storageNote = storageNote;
    }

    /** BR07: chi doi don vi khi chua co giao dich kho; service kiem tra va truyen hasTransactions. */
    public void changeUnit(String newUnit, boolean hasTransactions) {
        if (!unit.equals(newUnit) && hasTransactions) {
            throw new BusinessRuleException("BR07", "Nguyên liệu đã có giao dịch kho, không đổi được đơn vị tính");
        }
        this.unit = newUnit.trim();
    }

    public void setStatus(ActiveStatus status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public String getGroupName() { return groupName; }
    public String getUnit() { return unit; }
    public BigDecimal getMinStock() { return minStock; }
    public boolean isTrackExpiry() { return trackExpiry; }
    public String getStorageNote() { return storageNote; }
    public ActiveStatus getStatus() { return status; }
}
