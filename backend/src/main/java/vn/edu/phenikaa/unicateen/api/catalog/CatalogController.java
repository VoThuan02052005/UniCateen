package vn.edu.phenikaa.unicateen.api.catalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.phenikaa.unicateen.api.common.StatusRequest;
import vn.edu.phenikaa.unicateen.application.catalog.*;
import vn.edu.phenikaa.unicateen.application.catalog.CatalogViews.*;
import vn.edu.phenikaa.unicateen.application.catalog.RecipeService.RecipeLine;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;
import vn.edu.phenikaa.unicateen.domain.enums.ActiveStatus;
import vn.edu.phenikaa.unicateen.domain.enums.MenuItemStatus;

/** Nhom 1: danh muc, mon, gia, nguyen lieu, cong thuc (UC1.1-UC1.5). Quyen theo SecurityConfig. */
@RestController
@RequestMapping("/api/catalog")
public class CatalogController {
    private final CategoryService categories;
    private final MenuItemService items;
    private final PriceService prices;
    private final IngredientService ingredients;
    private final RecipeService recipes;

    public CatalogController(CategoryService categories, MenuItemService items, PriceService prices,
                             IngredientService ingredients, RecipeService recipes) {
        this.categories = categories;
        this.items = items;
        this.prices = prices;
        this.ingredients = ingredients;
        this.recipes = recipes;
    }

    // DV06: ten 2-100 ky tu, khong chi gom khoang trang
    public record CategoryRequest(@NotBlank @Size(min = 2, max = 100) String name, @PositiveOrZero int displayOrder,
                                  @Size(max = 255) String description) {
    }

    public record MenuItemRequest(@NotNull Long categoryId, @NotBlank @Size(min = 2, max = 100) String name,
                                  @NotBlank @Size(max = 20) String unit, @Size(max = 255) String imageUrl,
                                  @Size(max = 255) String description, @Positive Long initialPrice) {
    }

    public record PriceRequest(@NotNull @Positive Long price, LocalDateTime effectiveFrom,
                               @Size(max = 255) String reason) {
    }

    public record IngredientRequest(@NotBlank @Size(min = 2, max = 100) String name,
                                    @NotBlank @Size(max = 50) String groupName, @NotBlank @Size(max = 10) String unit,
                                    @NotNull @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal minStock,
                                    boolean trackExpiry, @Size(max = 255) String storageNote) {
    }

    public record RecipeLineRequest(@NotNull Long ingredientId,
                                    @NotNull @Positive @Digits(integer = 7, fraction = 3) BigDecimal qtyPerPortion) {
    }

    public record RecipeRequest(String note, @NotEmpty List<@Valid RecipeLineRequest> lines, boolean activate) {
    }

    // ---------------------------------------------------------------- UC1.1
    @GetMapping("/categories")
    public List<CategoryView> categories() {
        return categories.list();
    }

    @PostMapping("/categories")
    public CategoryView createCategory(@Valid @RequestBody CategoryRequest r) {
        return categories.create(r.name(), r.displayOrder(), r.description());
    }

    @PutMapping("/categories/{id}")
    public CategoryView updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest r) {
        return categories.update(id, r.name(), r.displayOrder(), r.description());
    }

    @PatchMapping("/categories/{id}/status")
    public CategoryView categoryStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return categories.setStatus(id, ActiveStatus.valueOf(r.status()));
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categories.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---------------------------------------------------------------- UC1.2
    @GetMapping("/menu-items")
    public List<MenuItemView> menuItems() {
        return items.list(false);
    }

    /** Danh sach cho giao dien ban hang: chi mon Dang ban co gia (BR03). */
    @GetMapping("/menu-items/on-sale")
    public List<MenuItemView> onSale() {
        return items.list(true);
    }

    @PostMapping("/menu-items")
    public MenuItemView createItem(@Valid @RequestBody MenuItemRequest r, @AuthenticationPrincipal CurrentUser me) {
        if (r.initialPrice() == null) {
            throw new IllegalArgumentException("Món mới phải có giá bán ban đầu (BR02)");
        }
        return items.create(r.categoryId(), r.name(), r.unit(), r.imageUrl(), r.description(), r.initialPrice(), me);
    }

    @PutMapping("/menu-items/{id}")
    public MenuItemView updateItem(@PathVariable Long id, @Valid @RequestBody MenuItemRequest r) {
        return items.update(id, r.categoryId(), r.name(), r.unit(), r.imageUrl(), r.description());
    }

    /** Dang ban, Tam het (ca nhan vien che bien), Ngung ban. */
    @PatchMapping("/menu-items/{id}/status")
    public MenuItemView itemStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest r,
                                   @AuthenticationPrincipal CurrentUser me) {
        MenuItemStatus s = MenuItemStatus.valueOf(r.status());
        if (me.role() == vn.edu.phenikaa.unicateen.domain.enums.Role.KITCHEN_STAFF && s == MenuItemStatus.DISCONTINUED) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Nhân viên chế biến chỉ đổi Đang bán / Tạm hết");
        }
        return items.changeStatus(id, s);
    }

    // ---------------------------------------------------------------- UC1.3
    @GetMapping("/menu-items/{id}/prices")
    public List<PriceView> priceHistory(@PathVariable Long id) {
        return prices.history(id);
    }

    @PostMapping("/menu-items/{id}/prices")
    public PriceView changePrice(@PathVariable Long id, @Valid @RequestBody PriceRequest r,
                                 @AuthenticationPrincipal CurrentUser me) {
        return prices.changePrice(id, r.price(), r.effectiveFrom(), r.reason(), me);
    }

    // ---------------------------------------------------------------- UC1.4
    @GetMapping("/ingredients")
    public List<IngredientView> ingredients() {
        return ingredients.list();
    }

    @PostMapping("/ingredients")
    public IngredientView createIngredient(@Valid @RequestBody IngredientRequest r) {
        return ingredients.create(r.name(), r.groupName(), r.unit(), r.minStock(), r.trackExpiry(), r.storageNote());
    }

    @PutMapping("/ingredients/{id}")
    public IngredientView updateIngredient(@PathVariable Long id, @Valid @RequestBody IngredientRequest r) {
        return ingredients.update(id, r.name(), r.groupName(), r.unit(), r.minStock(), r.trackExpiry(),
                r.storageNote());
    }

    @PatchMapping("/ingredients/{id}/status")
    public IngredientView ingredientStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest r) {
        return ingredients.setStatus(id, ActiveStatus.valueOf(r.status()));
    }

    // ---------------------------------------------------------------- UC1.5
    @GetMapping("/recipes/{menuItemId}")
    public List<RecipeView> recipeVersions(@PathVariable Long menuItemId) {
        return recipes.versions(menuItemId);
    }

    @PostMapping("/recipes/{menuItemId}")
    public RecipeView saveRecipe(@PathVariable Long menuItemId, @Valid @RequestBody RecipeRequest r) {
        return recipes.saveNewVersion(menuItemId, r.note(),
                r.lines().stream().map(l -> new RecipeLine(l.ingredientId(), l.qtyPerPortion())).toList(),
                r.activate());
    }

    @PostMapping("/recipes/{menuItemId}/versions/{version}/activate")
    public RecipeView activateRecipe(@PathVariable Long menuItemId, @PathVariable int version) {
        return recipes.activateVersion(menuItemId, version);
    }
}
