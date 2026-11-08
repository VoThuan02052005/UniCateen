package vn.edu.phenikaa.unicateen.application.port;

import java.util.List;
import java.util.Optional;
import vn.edu.phenikaa.unicateen.domain.enums.MenuItemStatus;
import vn.edu.phenikaa.unicateen.domain.model.MenuItem;

public interface MenuItemRepository {
    Optional<MenuItem> findById(Long id);

    List<MenuItem> findAllByOrderByCategoryDisplayOrderAscNameAsc();

    List<MenuItem> findByStatusOrderByCategoryDisplayOrderAscNameAsc(MenuItemStatus status);

    long countByCategoryId(Long categoryId);

    boolean existsByCategoryIdAndNameIgnoreCase(Long categoryId, String name);

    long count();

    <S extends MenuItem> S save(S item);
}
