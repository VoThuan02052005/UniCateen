package vn.edu.phenikaa.unicateen.api.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;
import vn.edu.phenikaa.unicateen.application.sales.OrderService;
import vn.edu.phenikaa.unicateen.application.sales.OrderService.LineInput;
import vn.edu.phenikaa.unicateen.application.sales.OrderView;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.domain.enums.OrderStatus;

/** Nhom 2: don hang (UC2.1-UC2.5). */
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    /** DV03: so luong nguyen 1-99. */
    public record LineRequest(@NotNull Long menuItemId, @Min(1) @Max(99) int quantity) {
    }

    public record QuantityRequest(@Min(1) @Max(99) int quantity) {
    }

    public record CreateRequest(List<@Valid LineRequest> lines) {
    }

    public record CancelRequest(@Size(max = 255) String reason) {
    }

    @PostMapping
    public OrderView create(@Valid @RequestBody(required = false) CreateRequest body,
                            @AuthenticationPrincipal CurrentUser me) {
        List<LineInput> lines = body == null || body.lines() == null ? List.of()
                : body.lines().stream().map(l -> new LineInput(l.menuItemId(), l.quantity())).toList();
        return orders.create(lines, me);
    }

    @GetMapping
    public List<OrderView> search(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                  LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                  LocalDate to,
                                  @RequestParam(required = false) OrderStatus status,
                                  @AuthenticationPrincipal CurrentUser me) {
        LocalDate t = to == null ? Clock.today() : to;
        return orders.search(from == null ? t : from, t, status, me);
    }

    /** UC2.5 Lich su mua hang ca nhan (STUDENT, FACULTY). */
    @GetMapping("/mine")
    public List<OrderView> mine(@AuthenticationPrincipal CurrentUser me) {
        return orders.myHistory(me);
    }

    @GetMapping("/{code}")
    public OrderView get(@PathVariable String code, @AuthenticationPrincipal CurrentUser me) {
        return orders.get(code, me);
    }

    @PostMapping("/{code}/lines")
    public OrderView addItem(@PathVariable String code, @Valid @RequestBody LineRequest r,
                             @AuthenticationPrincipal CurrentUser me) {
        return orders.addItem(code, new LineInput(r.menuItemId(), r.quantity()), me);
    }

    @PutMapping("/{code}/lines/{menuItemId}")
    public OrderView changeQuantity(@PathVariable String code, @PathVariable Long menuItemId,
                                    @Valid @RequestBody QuantityRequest r, @AuthenticationPrincipal CurrentUser me) {
        return orders.changeQuantity(code, menuItemId, r.quantity(), me);
    }

    @DeleteMapping("/{code}/lines/{menuItemId}")
    public OrderView removeItem(@PathVariable String code, @PathVariable Long menuItemId,
                                @AuthenticationPrincipal CurrentUser me) {
        return orders.removeItem(code, menuItemId, me);
    }

    @PostMapping("/{code}/submit")
    public OrderView submit(@PathVariable String code, @AuthenticationPrincipal CurrentUser me) {
        return orders.submit(code, me);
    }

    @PostMapping("/{code}/reopen")
    public OrderView reopen(@PathVariable String code, @AuthenticationPrincipal CurrentUser me) {
        return orders.reopen(code, me);
    }

    @PostMapping("/{code}/cancel")
    public OrderView cancel(@PathVariable String code, @Valid @RequestBody(required = false) CancelRequest r,
                            @AuthenticationPrincipal CurrentUser me) {
        return orders.cancel(code, r == null ? null : r.reason(), me);
    }
}
