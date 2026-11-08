package vn.edu.phenikaa.unicateen.application.catalog;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phenikaa.unicateen.application.catalog.CatalogViews.CategoryView;
import vn.edu.phenikaa.unicateen.application.common.CodeGenerator;
import vn.edu.phenikaa.unicateen.application.common.NotFoundException;
import vn.edu.phenikaa.unicateen.application.port.CategoryRepository;
import vn.edu.phenikaa.unicateen.application.port.MenuItemRepository;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.enums.ActiveStatus;
import vn.edu.phenikaa.unicateen.domain.model.Category;

/** UC1.1 Quan ly danh muc mon. */
@Service
@Transactional
public class CategoryService {
    private final CategoryRepository categories;
    private final MenuItemRepository items;

    public CategoryService(CategoryRepository categories, MenuItemRepository items) {
        this.categories = categories;
        this.items = items;
    }

    @Transactional(readOnly = true)
    public List<CategoryView> list() {
        return categories.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(c -> CategoryView.of(c, items.countByCategoryId(c.getId())))
                .toList();
    }

    public CategoryView create(String name, int displayOrder, String description) {
        if (categories.existsByNameIgnoreCase(name.trim())) {
            throw new BusinessRuleException("BR01", "Tên danh mục đã tồn tại");
        }
        List<String> codes = categories.findAllByOrderByDisplayOrderAscIdAsc().stream().map(Category::getCode).toList();
        Category c = categories.save(new Category(CodeGenerator.sequential("DM", 2, codes), name, displayOrder,
                description));
        return CategoryView.of(c, 0);
    }

    public CategoryView update(Long id, String name, int displayOrder, String description) {
        Category c = get(id);
        if (categories.existsByNameIgnoreCaseAndIdNot(name.trim(), id)) {
            throw new BusinessRuleException("BR01", "Tên danh mục đã tồn tại");
        }
        c.update(name, displayOrder, description);
        return CategoryView.of(c, items.countByCategoryId(id));
    }

    public CategoryView setStatus(Long id, ActiveStatus status) {
        Category c = get(id);
        c.setStatus(status);
        return CategoryView.of(c, items.countByCategoryId(id));
    }

    /** BR01: danh muc dang co mon khong duoc xoa, chi chuyen Ngung su dung. */
    public void delete(Long id) {
        Category c = get(id);
        if (items.countByCategoryId(id) > 0) {
            throw new BusinessRuleException("BR01", "Danh mục đang có món, chỉ được chuyển sang Ngừng sử dụng");
        }
        categories.delete(c);
    }

    Category get(Long id) {
        return categories.findById(id).orElseThrow(() -> new NotFoundException("danh mục", id));
    }
}
