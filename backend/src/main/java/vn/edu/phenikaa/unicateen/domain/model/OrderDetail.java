package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;

/** Dong don hang: so luong 1-99, giu don gia luc them (BR05, BR11, DV03). */
@Entity
@Table(name = "order_details")
public class OrderDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "unit_price", nullable = false)
    private long unitPrice;
    @Column(name = "line_total", nullable = false)
    private long lineTotal;

    protected OrderDetail() {
    }

    OrderDetail(Order order, MenuItem item, int quantity, long unitPrice) {
        this.order = order;
        this.menuItem = item;
        this.unitPrice = unitPrice;
        changeQuantity(quantity);
    }

    void changeQuantity(int qty) {
        if (qty < 1 || qty > Order.MAX_QTY) {
            throw new BusinessRuleException("BR11", "Số lượng phải từ 1 đến 99");
        }
        this.quantity = qty;
        this.lineTotal = qty * unitPrice;
    }

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public MenuItem getMenuItem() { return menuItem; }
    public int getQuantity() { return quantity; }
    public long getUnitPrice() { return unitPrice; }
    public long getLineTotal() { return lineTotal; }
}
