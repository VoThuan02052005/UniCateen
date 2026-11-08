package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.domain.enums.MenuItemStatus;

/** Mon an, do uong (UC1.2). Gia ban nam o ItemPrice (BR02, BR05). */
@Entity
@Table(name = "menu_items")
public class MenuItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 10)
    private String code;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(nullable = false, length = 20)
    private String unit;
    @Column(name = "image_url")
    private String imageUrl;
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MenuItemStatus status;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected MenuItem() {
    }

    public MenuItem(String code, Category category, String name, String unit, String imageUrl, String description) {
        this.code = code;
        this.status = MenuItemStatus.ON_SALE;
        this.createdAt = Clock.now();
        update(category, name, unit, imageUrl, description);
    }

    public void update(Category category, String name, String unit, String imageUrl, String description) {
        this.category = category;
        this.name = name.trim();
        this.unit = unit.trim();
        this.imageUrl = imageUrl;
        this.description = description;
    }

    /** BR03: chi mon Dang ban duoc them vao don. */
    public void ensureOnSale() {
        if (status != MenuItemStatus.ON_SALE) {
            throw new BusinessRuleException("BR03", "Món " + name + " không ở trạng thái Đang bán");
        }
    }

    /** Dang ban, Tam het, Ngung ban; mon da ban khong xoa, chi Ngung ban (BR04). */
    public void changeStatus(MenuItemStatus newStatus) {
        this.status = newStatus;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public Category getCategory() { return category; }
    public String getName() { return name; }
    public String getUnit() { return unit; }
    public String getImageUrl() { return imageUrl; }
    public String getDescription() { return description; }
    public MenuItemStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
