package vn.edu.phenikaa.unicateen.application.sales;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phenikaa.unicateen.application.catalog.PriceService;
import vn.edu.phenikaa.unicateen.application.common.Actors;
import vn.edu.phenikaa.unicateen.application.common.CodeGenerator;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;
import vn.edu.phenikaa.unicateen.application.common.ForbiddenException;
import vn.edu.phenikaa.unicateen.application.common.NotFoundException;
import vn.edu.phenikaa.unicateen.application.port.IAuditLogger;
import vn.edu.phenikaa.unicateen.application.port.MenuItemRepository;
import vn.edu.phenikaa.unicateen.application.port.OrderRepository;
import vn.edu.phenikaa.unicateen.application.port.PaymentRepository;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.enums.OrderStatus;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentMethod;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentStatus;
import vn.edu.phenikaa.unicateen.domain.enums.Role;
import vn.edu.phenikaa.unicateen.domain.model.MenuItem;
import vn.edu.phenikaa.unicateen.domain.model.Order;
import vn.edu.phenikaa.unicateen.domain.model.Payment;
import vn.edu.phenikaa.unicateen.domain.model.User;

/** Nhom 2: tao, dieu chinh, huy, tra cuu don hang (UC2.1-UC2.5). */
@Service
@Transactional
public class OrderService {
    private final OrderRepository orders;
    private final PaymentRepository payments;
    private final MenuItemRepository items;
    private final PriceService prices;
    private final CodeGenerator codes;
    private final Actors actors;
    private final IAuditLogger audit;

    public OrderService(OrderRepository orders, PaymentRepository payments, MenuItemRepository items,
                        PriceService prices, CodeGenerator codes, Actors actors, IAuditLogger audit) {
        this.orders = orders;
        this.payments = payments;
        this.items = items;
        this.prices = prices;
        this.codes = codes;
        this.actors = actors;
        this.audit = audit;
    }

    public record LineInput(Long menuItemId, int quantity) {
    }

    /** UC2.1: tao don DRAFT (BR10), co the kem cac dong ban dau. */
    public OrderView create(List<LineInput> lines, CurrentUser actor) {
        User cashier = actors.load(actor);
        Order o = new Order(codes.next("DH", 4, orders::findMaxCodeWithPrefix), cashier);
        if (lines != null) {
            for (LineInput l : lines) {
                addLine(o, l);
            }
        }
        return view(orders.save(o));
    }

    public OrderView addItem(String code, LineInput line, CurrentUser actor) {
        Order o = loadForUpdate(code, actor);
        addLine(o, line);
        return view(o);
    }

    public OrderView changeQuantity(String code, Long menuItemId, int quantity, CurrentUser actor) {
        Order o = loadForUpdate(code, actor);
        o.changeQuantity(menuItemId, quantity);
        return view(o);
    }

    public OrderView removeItem(String code, Long menuItemId, CurrentUser actor) {
        Order o = loadForUpdate(code, actor);
        o.removeItem(menuItemId);
        return view(o);
    }

    /** DRAFT -> PENDING_PAYMENT (BR12). */
    public OrderView submit(String code, CurrentUser actor) {
        Order o = loadForUpdate(code, actor);
        o.submit();
        return view(o);
    }

    /** UC2.2: PENDING_PAYMENT -> DRAFT, chi khi khong co giao dich PENDING (BR13). */
    public OrderView reopen(String code, CurrentUser actor) {
        Order o = loadForUpdate(code, actor);
        ensureNoPendingPayment(o, "BR13");
        o.reopen();
        return view(o);
    }

    /** UC2.3: huy don; don cho thanh toan phai co ly do va duoc ghi nhat ky (BR15). */
    public OrderView cancel(String code, String reason, CurrentUser actor) {
        Order o = loadForUpdate(code, actor);
        ensureNoPendingPayment(o, "BR15");
        OrderStatus before = o.getStatus();
        User by = actors.load(actor);
        o.cancel(reason, by);
        if (before == OrderStatus.PENDING_PAYMENT) {
            audit.log(by, "ORDER_CANCEL", "orders", o.getId(), Map.of("status", before.name()),
                    Map.of("status", o.getStatus().name(), "reason", reason));
        }
        return view(o);
    }

    @Transactional(readOnly = true)
    public OrderView get(String code, CurrentUser actor) {
        Order o = orders.findByCode(code).orElseThrow(() -> new NotFoundException("đơn", code));
        checkCanView(o, actor);
        return view(o);
    }

    /** UC2.4: nhan vien ban hang xem don minh tao, quan ly xem moi don (BR17). */
    @Transactional(readOnly = true)
    public List<OrderView> search(LocalDate from, LocalDate to, OrderStatus status, CurrentUser actor) {
        var start = from.atStartOfDay();
        var end = to.plusDays(1).atStartOfDay();
        List<Order> list = actor.is(Role.MANAGER)
                ? orders.findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(start, end)
                : orders.findByCashierIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
                        actor.id(), start, end);
        return list.stream().filter(o -> status == null || o.getStatus() == status).map(this::view).toList();
    }

    /** UC2.5: lich su mua hang ca nhan, ca ba hinh thuc thanh toan (BR16, BR17). */
    @Transactional(readOnly = true)
    public List<OrderView> myHistory(CurrentUser actor) {
        return orders.findByCustomerIdOrderByCreatedAtDesc(actor.id()).stream().map(this::view).toList();
    }

    // ------------------------------------------------------------------

    private void addLine(Order o, LineInput l) {
        MenuItem item = items.findById(l.menuItemId())
                .orElseThrow(() -> new NotFoundException("món", l.menuItemId()));
        o.addItem(item, l.quantity(), prices.currentPrice(item.getId()));
    }

    private Order loadForUpdate(String code, CurrentUser actor) {
        Order o = orders.findByCodeForUpdate(code).orElseThrow(() -> new NotFoundException("đơn", code));
        checkCanModify(o, actor);
        return o;
    }

    private void ensureNoPendingPayment(Order o, String rule) {
        if (payments.existsByOrderIdAndStatusIn(o.getId(), EnumSet.of(PaymentStatus.PENDING))) {
            throw new BusinessRuleException(rule, "Đơn đang có giao dịch thanh toán chờ xử lý");
        }
    }

    /** Lop kiem tra thu hai: quyen so huu du lieu (BR17). */
    void checkCanView(Order o, CurrentUser actor) {
        boolean ok = switch (actor.role()) {
            case MANAGER -> true;
            case CASHIER -> o.isOwnedBy(actor.id());
            case STUDENT, FACULTY -> o.getCustomer() != null && o.getCustomer().getId().equals(actor.id());
            default -> false;
        };
        if (!ok) {
            throw new ForbiddenException("Không có quyền xem đơn này");
        }
    }

    void checkCanModify(Order o, CurrentUser actor) {
        if (!(actor.is(Role.MANAGER) || (actor.is(Role.CASHIER) && o.isOwnedBy(actor.id())))) {
            throw new ForbiddenException("Không có quyền thao tác trên đơn này");
        }
    }

    OrderView view(Order o) {
        PaymentMethod paidBy = o.getStatus() == OrderStatus.PAID
                ? payments.findByOrderIdOrderByCreatedAtDesc(o.getId()).stream()
                        .filter(p -> p.getStatus() == PaymentStatus.SUCCESS).map(Payment::getMethod)
                        .findFirst().orElse(null)
                : null;
        return OrderView.of(o, paidBy);
    }
}
