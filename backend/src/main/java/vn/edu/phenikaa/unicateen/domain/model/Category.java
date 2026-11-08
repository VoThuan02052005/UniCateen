package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import vn.edu.phenikaa.unicateen.domain.enums.ActiveStatus;

/** Danh muc mon (UC1.1). Danh muc dang co mon khong xoa, chi ngung su dung (BR01). */
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 10)
    private String code;
    @Column(nullable = false, length = 100)
    private String name;
    @Column(name = "display_order", nullable = false)
    private int displayOrder;
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActiveStatus status;

    protected Category() {
    }

    public Category(String code, String name, int displayOrder, String description) {
        this.code = code;
        this.status = ActiveStatus.ACTIVE;
        update(name, displayOrder, description);
    }

    public void update(String name, int displayOrder, String description) {
        this.name = name.trim();
        this.displayOrder = displayOrder;
        this.description = description;
    }

    public void setStatus(ActiveStatus status) {
        this.status = status;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public int getDisplayOrder() { return displayOrder; }
    public String getDescription() { return description; }
    public ActiveStatus getStatus() { return status; }
}
