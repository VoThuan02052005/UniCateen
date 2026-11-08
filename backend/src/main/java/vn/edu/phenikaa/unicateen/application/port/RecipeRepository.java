package vn.edu.phenikaa.unicateen.application.port;

import java.util.List;
import java.util.Optional;
import vn.edu.phenikaa.unicateen.domain.enums.RecipeStatus;
import vn.edu.phenikaa.unicateen.domain.model.Recipe;

public interface RecipeRepository {
    Optional<Recipe> findByMenuItemIdAndStatus(Long menuItemId, RecipeStatus status);

    List<Recipe> findByMenuItemIdOrderByVersionDesc(Long menuItemId);

    <S extends Recipe> S save(S recipe);

    void flush();
}
