package vn.edu.phenikaa.unicateen.application.port;

import java.util.List;
import java.util.Optional;
import vn.edu.phenikaa.unicateen.domain.model.Ingredient;

public interface IngredientRepository {
    Optional<Ingredient> findById(Long id);

    List<Ingredient> findAllByOrderByGroupNameAscNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    long count();

    <S extends Ingredient> S save(S ingredient);
}
