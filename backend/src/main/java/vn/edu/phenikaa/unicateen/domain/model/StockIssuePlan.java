package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;

/** So phan du kien theo mon cua mot phieu xuat; dau vao cua buoc goi y (UC4.4). */
@Entity
@Table(name = "stock_issue_plans")
public class StockIssuePlan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id")
    private StockIssue issue;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;
    @Column(name = "planned_portions", nullable = false)
    private int plannedPortions;

    protected StockIssuePlan() {
    }

    StockIssuePlan(StockIssue issue, MenuItem item, int portions) {
        this.issue = issue;
        this.menuItem = item;
        this.plannedPortions = portions;
    }

    public Long getId() { return id; }
    public StockIssue getIssue() { return issue; }
    public MenuItem getMenuItem() { return menuItem; }
    public int getPlannedPortions() { return plannedPortions; }
}
