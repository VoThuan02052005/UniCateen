package vn.edu.phenikaa.unicateen.application.sales;

import java.time.LocalDateTime;
import java.util.List;
import vn.edu.phenikaa.unicateen.domain.enums.OrderStatus;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentMethod;
import vn.edu.phenikaa.unicateen.domain.model.Order;

/** Don hang tra ve giao dien; {@code paymentMethod} la hinh thuc cua giao dich SUCCESS (neu co). */
public record OrderView(Long id, String code, OrderStatus status, long totalAmount, String cashierName,
                        String customerName, String cancelReason, LocalDateTime createdAt, LocalDateTime paidAt,
                        PaymentMethod paymentMethod, List<Line> lines) {

    public record Line(Long menuItemId, String menuItemName, String unit, int quantity, long unitPrice,
                       long lineTotal) {
    }

    public static OrderView of(Order o, PaymentMethod paidBy) {
        List<Line> lines = o.getDetails().stream()
                .map(d -> new Line(d.getMenuItem().getId(), d.getMenuItem().getName(), d.getMenuItem().getUnit(),
                        d.getQuantity(), d.getUnitPrice(), d.getLineTotal()))
                .toList();
        return new OrderView(o.getId(), o.getCode(), o.getStatus(), o.getTotalAmount(),
                o.getCashier().getFullName(), o.getCustomer() == null ? null : o.getCustomer().getFullName(),
                o.getCancelReason(), o.getCreatedAt(), o.getPaidAt(), paidBy, lines);
    }
}
