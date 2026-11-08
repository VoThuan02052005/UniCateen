package vn.edu.phenikaa.unicateen.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.phenikaa.unicateen.application.port.IngredientRepository;
import vn.edu.phenikaa.unicateen.domain.model.Ingredient;

public interface JpaIngredientRepository extends JpaRepository<Ingredient, Long>, IngredientRepository {
}
