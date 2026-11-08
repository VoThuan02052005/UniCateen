package vn.edu.phenikaa.unicateen.application.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import vn.edu.phenikaa.unicateen.domain.enums.ActiveStatus;
import vn.edu.phenikaa.unicateen.domain.enums.MenuItemStatus;
import vn.edu.phenikaa.unicateen.domain.enums.PriceStatus;
import vn.edu.phenikaa.unicateen.domain.enums.RecipeStatus;
import vn.edu.phenikaa.unicateen.domain.model.Category;
import vn.edu.phenikaa.unicateen.domain.model.Ingredient;
import vn.edu.phenikaa.unicateen.domain.model.ItemPrice;
import vn.edu.phenikaa.unicateen.domain.model.MenuItem;
import vn.edu.phenikaa.unicateen.domain.model.Recipe;

/** Du lieu tra ve cho giao dien nhom danh muc va thuc don. */
public final class CatalogViews {
    private CatalogViews() {
    }

    public record CategoryView(Long id, String code, String name, int displayOrder, String description,
                               ActiveStatus status, long itemCount) {
        public static CategoryView of(Category c, long itemCount) {
            return new CategoryView(c.getId(), c.getCode(), c.getName(), c.getDisplayOrder(), c.getDescription(),
                    c.getStatus(), itemCount);
        }
    }

    public record MenuItemView(Long id, String code, Long categoryId, String categoryName, String name, String unit,
                               String imageUrl, String description, MenuItemStatus status, Long price) {
        public static MenuItemView of(MenuItem m, Long price) {
            return new MenuItemView(m.getId(), m.getCode(), m.getCategory().getId(), m.getCategory().getName(),
                    m.getName(), m.getUnit(), m.getImageUrl(), m.getDescription(), m.getStatus(), price);
        }
    }

    public record PriceView(Long id, long price, LocalDateTime effectiveFrom, LocalDateTime effectiveTo,
                            String reason, PriceStatus status, String createdBy) {
        public static PriceView of(ItemPrice p) {
            return new PriceView(p.getId(), p.getPrice(), p.getEffectiveFrom(), p.getEffectiveTo(), p.getReason(),
                    p.getStatus(), p.getCreatedBy().getFullName());
        }
    }

    public record IngredientView(Long id, String code, String name, String groupName, String unit,
                                 BigDecimal minStock, boolean trackExpiry, String storageNote, ActiveStatus status) {
        public static IngredientView of(Ingredient i) {
            return new IngredientView(i.getId(), i.getCode(), i.getName(), i.getGroupName(), i.getUnit(),
                    i.getMinStock(), i.isTrackExpiry(), i.getStorageNote(), i.getStatus());
        }
    }

    public record RecipeLineView(Long ingredientId, String ingredientName, String unit, BigDecimal qtyPerPortion) {
    }

    public record RecipeView(Long id, Long menuItemId, String menuItemName, int version, RecipeStatus status,
                             String note, LocalDateTime createdAt, List<RecipeLineView> lines) {
        public static RecipeView of(Recipe r) {
            List<RecipeLineView> lines = r.getDetails().stream()
                    .map(d -> new RecipeLineView(d.getIngredient().getId(), d.getIngredient().getName(),
                            d.getIngredient().getUnit(), d.getQtyPerPortion()))
                    .toList();
            return new RecipeView(r.getId(), r.getMenuItem().getId(), r.getMenuItem().getName(), r.getVersion(),
                    r.getStatus(), r.getNote(), r.getCreatedAt(), lines);
        }
    }
}
