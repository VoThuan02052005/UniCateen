package vn.edu.phenikaa.unicateen.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.phenikaa.unicateen.application.port.CategoryRepository;
import vn.edu.phenikaa.unicateen.domain.model.Category;

public interface JpaCategoryRepository extends JpaRepository<Category, Long>, CategoryRepository {
}
