package vn.edu.phenikaa.unicateen.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.phenikaa.unicateen.application.port.MenuItemRepository;
import vn.edu.phenikaa.unicateen.domain.model.MenuItem;

public interface JpaMenuItemRepository extends JpaRepository<MenuItem, Long>, MenuItemRepository {
}
