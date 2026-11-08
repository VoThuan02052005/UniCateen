package vn.edu.phenikaa.unicateen.application.catalog;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phenikaa.unicateen.application.catalog.CatalogViews.RecipeView;
import vn.edu.phenikaa.unicateen.application.port.RecipeRepository;
import vn.edu.phenikaa.unicateen.domain.enums.RecipeStatus;
import vn.edu.phenikaa.unicateen.domain.model.MenuItem;
import vn.edu.phenikaa.unicateen.domain.model.Recipe;

/** UC1.5 Quan ly cong thuc va dinh muc (GD7). Moi mon toi da mot cong thuc dang ap dung (BR09). */
@Service
@Transactional
public class RecipeService {
    private final RecipeRepository recipes;
    private final MenuItemService items;
    private final IngredientService ingredients;

    public RecipeService(RecipeRepository recipes, MenuItemService items, IngredientService ingredients) {
        this.recipes = recipes;
        this.items = items;
        this.ingredients = ingredients;
    }

    public record RecipeLine(Long ingredientId, BigDecimal qtyPerPortion) {
    }

    @Transactional(readOnly = true)
    public List<RecipeView> versions(Long menuItemId) {
        return recipes.findByMenuItemIdOrderByVersionDesc(menuItemId).stream().map(RecipeView::of).toList();
    }

    @Transactional(readOnly = true)
    public Optional<Recipe> activeRecipe(Long menuItemId) {
        return recipes.findByMenuItemIdAndStatus(menuItemId, RecipeStatus.ACTIVE);
    }

    /**
     * Luu phien ban cong thuc moi. Neu {@code activate}, phien ban cu ngung ap dung truoc roi phien ban moi moi
     * duoc ap dung; thu tu flush nay tranh vi pham chi muc UNIQUE active_item_id.
     */
    public RecipeView saveNewVersion(Long menuItemId, String note, List<RecipeLine> lines, boolean activate) {
        MenuItem item = items.get(menuItemId);
        int nextVersion = recipes.findByMenuItemIdOrderByVersionDesc(menuItemId).stream()
                .mapToInt(Recipe::getVersion).max().orElse(0) + 1;
        Recipe r = new Recipe(item, nextVersion, note);
        for (RecipeLine l : lines) {
            r.addDetail(ingredients.get(l.ingredientId()), l.qtyPerPortion());
        }
        recipes.save(r);
        recipes.flush();
        if (activate) {
            activate(r);
        }
        return RecipeView.of(r);
    }

    /** Ap dung lai mot phien ban cu. */
    public RecipeView activateVersion(Long menuItemId, int version) {
        Recipe r = recipes.findByMenuItemIdOrderByVersionDesc(menuItemId).stream()
                .filter(x -> x.getVersion() == version).findFirst()
                .orElseThrow(() -> new vn.edu.phenikaa.unicateen.application.common.NotFoundException(
                        "công thức phiên bản", version));
        activate(r);
        return RecipeView.of(r);
    }

    private void activate(Recipe r) {
        activeRecipe(r.getMenuItem().getId()).filter(old -> !old.getId().equals(r.getId())).ifPresent(old -> {
            old.retire();
            recipes.flush();
        });
        r.activate();
        recipes.flush();
    }
}
