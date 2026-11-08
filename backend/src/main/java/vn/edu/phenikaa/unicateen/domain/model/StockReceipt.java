package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.domain.enums.DocumentStatus;

/** Phieu nhap kho (UC4.2). Nhap -> Da xac nhan; sau xac nhan khong sua, khong xoa (BR31). */
@Entity
@Table(name = "stock_receipts")
public class StockReceipt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String code;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;
    @Column(name = "receipt_date", nullable = false)
    private LocalDate receiptDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;
    @Column(name = "total_value", nullable = false)
    private long totalValue;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    @OneToMany(mappedBy = "receipt", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<StockReceiptDetail> details = new ArrayList<>();

    protected StockReceipt() {
    }

    public StockReceipt(String code, Supplier supplier, LocalDate receiptDate, User createdBy) {
        if (!supplier.isActive()) {
            throw new BusinessRuleException("BR30", "Nhà cung cấp đã ngừng giao dịch");
        }
        this.code = code;
        this.supplier = supplier;
        this.receiptDate = receiptDate;
        this.createdBy = createdBy;
        this.status = DocumentStatus.DRAFT;
    }

    /** BR32: so luong > 0, don gia khong am, han dung khong som hon ngay nhap (bat buoc neu theo doi han). */
    public void addLine(Ingredient ingredient, BigDecimal quantity, long unitCost, LocalDate expiryDate) {
        requireDraft();
        if (quantity == null || quantity.signum() <= 0) {
            throw new BusinessRuleException("BR32", "Số lượng nhập phải lớn hơn 0");
        }
        if (unitCost < 0) {
            throw new BusinessRuleException("BR32", "Đơn giá nhập không được âm");
        }
        if (ingredient.isTrackExpiry() && expiryDate == null) {
            throw new BusinessRuleException("BR32", "Nguyên liệu " + ingredient.getName() + " bắt buộc có hạn sử dụng");
        }
        if (expiryDate != null && expiryDate.isBefore(receiptDate)) {
            throw new BusinessRuleException("BR32", "Hạn sử dụng sớm hơn ngày nhập");
        }
        details.add(new StockReceiptDetail(this, ingredient, quantity, unitCost, expiryDate));
        totalValue = details.stream().mapToLong(StockReceiptDetail::getLineTotal).sum();
    }

    public void clearLines() {
        requireDraft();
        details.clear();
        totalValue = 0;
    }

    /** Nhap -> Da xac nhan. Service tao lo va giao dich kho cho moi dong (BR33, GD6). */
    public void confirm() {
        requireDraft();
        if (details.isEmpty()) {
            throw new BusinessRuleException("BR32", "Phiếu nhập chưa có dòng nào");
        }
        status = DocumentStatus.CONFIRMED;
        confirmedAt = Clock.now();
    }

    public void cancel() {
        requireDraft();
        status = DocumentStatus.CANCELLED;
    }

    private void requireDraft() {
        if (status != DocumentStatus.DRAFT) {
            throw new BusinessRuleException("BR31", "Phiếu " + code + " đã xác nhận hoặc đã hủy, không sửa được");
        }
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public Supplier getSupplier() { return supplier; }
    public LocalDate getReceiptDate() { return receiptDate; }
    public DocumentStatus getStatus() { return status; }
    public long getTotalValue() { return totalValue; }
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public List<StockReceiptDetail> getDetails() { return details; }
}
