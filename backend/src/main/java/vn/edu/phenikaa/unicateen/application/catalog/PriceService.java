package vn.edu.phenikaa.unicateen.application.catalog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phenikaa.unicateen.application.catalog.CatalogViews.PriceView;
import vn.edu.phenikaa.unicateen.application.common.Actors;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;
import vn.edu.phenikaa.unicateen.application.common.NotFoundException;
import vn.edu.phenikaa.unicateen.application.port.IAuditLogger;
import vn.edu.phenikaa.unicateen.application.port.ItemPriceRepository;
import vn.edu.phenikaa.unicateen.application.port.MenuItemRepository;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.domain.model.ItemPrice;
import vn.edu.phenikaa.unicateen.domain.model.MenuItem;
import vn.edu.phenikaa.unicateen.domain.model.User;

/** UC1.3 Cap nhat gia ban. Doi gia tao ban ghi moi (BR05), chi MANAGER, co nhat ky (BR06). */
@Service
@Transactional
public class PriceService {
    private final ItemPriceRepository prices;
    private final MenuItemRepository items;
    private final Actors actors;
    private final IAuditLogger audit;

    public PriceService(ItemPriceRepository prices, MenuItemRepository items, Actors actors, IAuditLogger audit) {
        this.prices = prices;
        this.items = items;
        this.actors = actors;
        this.audit = audit;
    }

    /** Gia hien hanh; null neu mon chua co gia. */
    @Transactional(readOnly = true)
    public Long currentPriceOrNull(Long menuItemId) {
        return prices.findEffectiveAt(menuItemId, Clock.now()).map(ItemPrice::getPrice).orElse(null);
    }

    /** Gia hien hanh, dung khi them mon vao don (BR11). */
    @Transactional(readOnly = true)
    public long currentPrice(Long menuItemId) {
        Long p = currentPriceOrNull(menuItemId);
        if (p == null) {
            throw new BusinessRuleException("BR02", "Món chưa có giá bán hiện hành");
        }
        return p;
    }

    @Transactional(readOnly = true)
    public List<PriceView> history(Long menuItemId) {
        return prices.findByMenuItemIdOrderByEffectiveFromDesc(menuItemId).stream().map(PriceView::of).toList();
    }

    /** Dat gia moi tu thoi diem {@code from} (null = ngay bay gio); gia dang mo duoc dong tai thoi diem do. */
    public PriceView changePrice(Long menuItemId, long newPrice, LocalDateTime from, String reason, CurrentUser actor) {
        MenuItem item = items.findById(menuItemId).orElseThrow(() -> new NotFoundException("món", menuItemId));
        LocalDateTime now = Clock.now();
        LocalDateTime start = from == null || from.isBefore(now) ? now : from;
        User by = actors.load(actor);
        Long oldPrice = prices.findEffectiveAt(menuItemId, now).map(ItemPrice::getPrice).orElse(null);
        for (ItemPrice open : prices.findOpenFrom(menuItemId, start)) {
            if (!open.getEffectiveFrom().isBefore(start)) {
                open.supersede();              // gia bat dau tu moc moi tro di bi thay the
            } else {
                open.closeAt(start);
            }
        }
        ItemPrice p = prices.save(new ItemPrice(item, newPrice, start, reason, by, now));
        audit.log(by, "PRICE_CHANGE", "menu_items", menuItemId,
                oldPrice == null ? null : Map.of("price", oldPrice),
                Map.of("price", newPrice, "effectiveFrom", start.toString()));
        return PriceView.of(p);
    }

    /** Lich: chuyen gia hen gio sang ACTIVE khi den thoi diem hieu luc. */
    @Scheduled(fixedDelayString = "PT1M")
    public void activateDuePrices() {
        prices.findDueScheduled(Clock.now()).forEach(ItemPrice::activate);
    }
}
