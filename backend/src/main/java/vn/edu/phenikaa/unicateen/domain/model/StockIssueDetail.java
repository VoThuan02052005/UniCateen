package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Dong phieu xuat theo lo. */
@Entity
@Table(name = "stock_issue_details")
public class StockIssueDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id")
    private StockIssue issue;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lot_id")
    private StockLot lot;
    @Column(name = "suggested_qty", precision = 12, scale = 3)
    private BigDecimal suggestedQty;
    @Column(name = "issued_qty", nullable = false, precision = 12, scale = 3)
    private BigDecimal issuedQty;
    @Column(name = "outside_recipe", nullable = false)
    private boolean outsideRecipe;

    protected StockIssueDetail() {
    }

    StockIssueDetail(StockIssue issue, StockLot lot, BigDecimal suggested, BigDecimal issued, boolean outsideRecipe) {
        this.issue = issue;
        this.lot = lot;
        this.suggestedQty = suggested;
        this.issuedQty = issued;
        this.outsideRecipe = outsideRecipe;
    }

    public Long getId() { return id; }
    public StockIssue getIssue() { return issue; }
    public StockLot getLot() { return lot; }
    public BigDecimal getSuggestedQty() { return suggestedQty; }
    public BigDecimal getIssuedQty() { return issuedQty; }
    public boolean isOutsideRecipe() { return outsideRecipe; }
}
