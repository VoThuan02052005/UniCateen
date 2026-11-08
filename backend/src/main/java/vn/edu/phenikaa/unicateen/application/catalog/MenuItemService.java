package vn.edu.phenikaa.unicateen.application.catalog;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phenikaa.unicateen.application.catalog.CatalogViews.MenuItemView;
import vn.edu.phenikaa.unicateen.application.common.CodeGenerator;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;
import vn.edu.phenikaa.unicateen.application.common.NotFoundException;
import vn.edu.phenikaa.unicateen.application.port.MenuItemRepository;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.enums.ActiveStatus;
import vn.edu.phenikaa.unicateen.domain.enums.MenuItemStatus;
import vn.edu.phenikaa.unicateen.domain.model.Category;
import vn.edu.phenikaa.unicateen.domain.model.MenuItem;

/** UC1.2 Quan ly mon an va do uong. Mon khong bi xoa, chi Ngung ban (BR04). */
@Service
@Transactional
public class MenuItemService {
    private final MenuItemRepository items;
    private final CategoryService categories;
    private final PriceService prices;

    public MenuItemService(MenuItemRepository items, CategoryService categories, PriceService prices) {
        this.items = items;
        this.categories = categories;
        this.prices = prices;
    }

    @Transactional(readOnly = true)
    public List<MenuItemView> list(boolean onSaleOnly) {
        List<MenuItem> list = onSaleOnly
                ? items.findByStatusOrderByCategoryDisplayOrderAscNameAsc(MenuItemStatus.ON_SALE)
                : items.findAllByOrderByCategoryDisplayOrderAscNameAsc();
        return list.stream()
                .filter(m -> !onSaleOnly || m.getCategory().getStatus() == ActiveStatus.ACTIVE)
                .map(m -> MenuItemView.of(m, prices.currentPriceOrNull(m.getId())))
                .filter(v -> !onSaleOnly || v.price() != null)        // BR03: POS chi hien mon dang ban co gia
                .toList();
    }

    /** Tao mon cung gia ban dau (BR02: moi mon co gia rieng). */
    public MenuItemView create(Long categoryId, String name, String unit, String imageUrl, String description,
                               long initialPrice, CurrentUser actor) {
        Category c = categories.get(categoryId);
        if (items.existsByCategoryIdAndNameIgnoreCase(categoryId, name.trim())) {
            throw new BusinessRuleException("BR01", "Danh mục đã có món cùng tên");
        }
        List<String> codes = items.findAllByOrderByCategoryDisplayOrderAscNameAsc().stream()
                .map(MenuItem::getCode).toList();
        MenuItem m = items.save(new MenuItem(CodeGenerator.sequential("M", 3, codes), c, name, unit, imageUrl,
                description));
        prices.changePrice(m.getId(), initialPrice, null, "Giá ban đầu", actor);
        return MenuItemView.of(m, initialPrice);
    }

    public MenuItemView update(Long id, Long categoryId, String name, String unit, String imageUrl,
                               String description) {
        MenuItem m = get(id);
        m.update(categories.get(categoryId), name, unit, imageUrl, description);
        return MenuItemView.of(m, prices.currentPriceOrNull(id));
    }

    /** Dang ban, Tam het (nhan vien che bien duoc doi), Ngung ban. */
    public MenuItemView changeStatus(Long id, MenuItemStatus status) {
        MenuItem m = get(id);
        m.changeStatus(status);
        return MenuItemView.of(m, prices.currentPriceOrNull(id));
    }

    MenuItem get(Long id) {
        return items.findById(id).orElseThrow(() -> new NotFoundException("món", id));
    }
}
