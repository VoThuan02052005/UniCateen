package vn.edu.phenikaa.unicateen.application.port;

import java.util.List;
import java.util.Optional;
import vn.edu.phenikaa.unicateen.domain.model.Category;

public interface CategoryRepository {
    Optional<Category> findById(Long id);

    List<Category> findAllByOrderByDisplayOrderAscIdAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    long count();

    <S extends Category> S save(S category);

    void delete(Category category);
}
