package vn.edu.phenikaa.unicateen.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.phenikaa.unicateen.application.port.RecipeRepository;
import vn.edu.phenikaa.unicateen.domain.model.Recipe;

public interface JpaRecipeRepository extends JpaRepository<Recipe, Long>, RecipeRepository {
}
