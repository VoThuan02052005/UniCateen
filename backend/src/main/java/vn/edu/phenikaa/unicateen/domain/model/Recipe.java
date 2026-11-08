package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.domain.enums.RecipeStatus;

/** Cong thuc che bien va dinh muc (UC1.5, GD7). Moi mon toi da mot cong thuc ACTIVE (BR09). */
@Entity
@Table(name = "recipes")
public class Recipe {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_item_id")
    private MenuItem menuItem;
    @Column(nullable = false)
    private int version;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecipeStatus status;
    @Column(columnDefinition = "TEXT")
    private String note;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeDetail> details = new ArrayList<>();

    protected Recipe() {
    }

    /** Tao phien ban moi o trang thai RETIRED; service goi activate() de ap dung. */
    public Recipe(MenuItem item, int version, String note) {
        this.menuItem = item;
        this.version = version;
        this.note = note;
        this.status = RecipeStatus.RETIRED;
        this.createdAt = Clock.now();
    }

    /** BR08: dinh muc duong, moi nguyen lieu toi da mot lan. */
    public void addDetail(Ingredient ingredient, BigDecimal qtyPerPortion) {
        if (qtyPerPortion == null || qtyPerPortion.signum() <= 0) {
            throw new BusinessRuleException("BR08", "Định mức phải là số dương");
        }
        boolean dup = details.stream().anyMatch(d -> d.getIngredient().getId().equals(ingredient.getId()));
        if (dup) {
            throw new BusinessRuleException("BR08", "Nguyên liệu " + ingredient.getName() + " đã có trong công thức");
        }
        details.add(new RecipeDetail(this, ingredient, qtyPerPortion));
    }

    public void activate() {
        if (details.isEmpty()) {
            throw new BusinessRuleException("BR08", "Công thức chưa có nguyên liệu");
        }
        status = RecipeStatus.ACTIVE;
    }

    public void retire() {
        status = RecipeStatus.RETIRED;
    }

    public boolean isActive() {
        return status == RecipeStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public MenuItem getMenuItem() { return menuItem; }
    public int getVersion() { return version; }
    public RecipeStatus getStatus() { return status; }
    public String getNote() { return note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<RecipeDetail> getDetails() { return details; }
}
