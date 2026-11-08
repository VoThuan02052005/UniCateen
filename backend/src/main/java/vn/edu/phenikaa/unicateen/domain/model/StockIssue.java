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
import vn.edu.phenikaa.unicateen.domain.enums.Shift;

/** Phieu xuat nguyen lieu theo ca (UC4.4). Ton bi tru khi xac nhan (GD5, BR36). */
@Entity
@Table(name = "stock_issues")
public class StockIssue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 20)
    private String code;
    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Shift shift;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus status;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by")
    private User createdBy;
    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<StockIssuePlan> plans = new ArrayList<>();
    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<StockIssueDetail> details = new ArrayList<>();

    protected StockIssue() {
    }

    public StockIssue(String code, LocalDate issueDate, Shift shift, User createdBy) {
        this.code = code;
        this.issueDate = issueDate;
        this.shift = shift;
        this.createdBy = createdBy;
        this.status = DocumentStatus.DRAFT;
    }

    public void setPlan(MenuItem item, int portions) {
        requireDraft();
        if (portions <= 0) {
            throw new BusinessRuleException("DV04", "Số phần dự kiến phải lớn hơn 0");
        }
        plans.removeIf(p -> p.getMenuItem().getId().equals(item.getId()));
        plans.add(new StockIssuePlan(this, item, portions));
    }

    public void clearLines() {
        requireDraft();
        details.clear();
    }

    public void addLine(StockLot lot, BigDecimal suggested, BigDecimal issued, boolean outsideRecipe) {
        requireDraft();
        if (issued == null || issued.signum() <= 0) {
            throw new BusinessRuleException("DV04", "Lượng xuất phải lớn hơn 0");
        }
        details.add(new StockIssueDetail(this, lot, suggested, issued, outsideRecipe));
    }

    public void confirm() {
        requireDraft();
        if (details.isEmpty()) {
            throw new BusinessRuleException("BR33", "Phiếu xuất chưa có dòng nào");
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
    public LocalDate getIssueDate() { return issueDate; }
    public Shift getShift() { return shift; }
    public DocumentStatus getStatus() { return status; }
    public User getCreatedBy() { return createdBy; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public List<StockIssuePlan> getPlans() { return plans; }
    public List<StockIssueDetail> getDetails() { return details; }
}
