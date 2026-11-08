package vn.edu.phenikaa.unicateen.domain;

import static org.junit.jupiter.api.Assertions.*;
import static vn.edu.phenikaa.unicateen.support.Fixtures.*;

import org.junit.jupiter.api.Test;
import vn.edu.phenikaa.unicateen.domain.enums.MenuItemStatus;
import vn.edu.phenikaa.unicateen.domain.enums.OrderStatus;
import vn.edu.phenikaa.unicateen.domain.enums.Role;
import vn.edu.phenikaa.unicateen.domain.model.MenuItem;
import vn.edu.phenikaa.unicateen.domain.model.Order;
import vn.edu.phenikaa.unicateen.domain.model.User;

/** May trang thai don hang (TC-ORD, TC-ST): BR10-BR15, BR19, BR22. */
class OrderStateTest {
    private final User cashier = user(3, Role.CASHIER);

    @Test
    void newOrderIsDraft_BR10() {
        Order o = order("DH-261108-0001", cashier);
        assertEquals(OrderStatus.DRAFT, o.getStatus());
        assertEquals(0, o.getTotalAmount());
    }

    @Test
    void sameItemMergesIntoOneLine_andTotalIsSumOfLines_BR11_BR12() {
        Order o = order("DH-1", cashier);
        MenuItem com = item(1, "Cơm gà");
        o.addItem(com, 2, 35_000);
        o.addItem(com, 1, 35_000);
        o.addItem(item(2, "Trà đá"), 3, 3_000);
        assertEquals(2, o.getDetails().size());
        assertEquals(3, o.getDetails().get(0).getQuantity());
        assertEquals(3 * 35_000 + 3 * 3_000, o.getTotalAmount());
    }

    @Test
    void quantityOutside1To99IsRejected_DV03() {
        Order o = order("DH-1", cashier);
        assertEquals("BR11", assertThrows(BusinessRuleException.class,
                () -> o.addItem(item(1, "Cơm"), 0, 35_000)).getRule());
        o.addItem(item(1, "Cơm"), 99, 35_000);
        assertThrows(BusinessRuleException.class, () -> o.changeQuantity(1L, 100));
    }

    @Test
    void itemNotOnSaleCannotBeAdded_BR03() {
        MenuItem m = item(8, "Bánh mì");
        m.changeStatus(MenuItemStatus.OUT_OF_STOCK);
        Order o = order("DH-1", cashier);
        assertEquals("BR03", assertThrows(BusinessRuleException.class, () -> o.addItem(m, 1, 15_000)).getRule());
    }

    @Test
    void emptyOrderCannotBeSubmitted_BR12() {
        Order o = order("DH-1", cashier);
        assertEquals("BR12", assertThrows(BusinessRuleException.class, o::submit).getRule());
    }

    @Test
    void pendingOrderCannotBeEditedUntilReopened_BR13() {
        Order o = pendingOrder(cashier);
        assertThrows(BusinessRuleException.class, () -> o.addItem(item(2, "Trà đá"), 1, 3_000));
        o.reopen();
        assertEquals(OrderStatus.DRAFT, o.getStatus());
        o.addItem(item(2, "Trà đá"), 1, 3_000);
        assertEquals(73_000, o.getTotalAmount());
    }

    @Test
    void cancelPendingRequiresReason_BR15() {
        Order o = pendingOrder(cashier);
        assertThrows(BusinessRuleException.class, () -> o.cancel("  ", cashier));
        o.cancel("Khách đổi ý", cashier);
        assertEquals(OrderStatus.CANCELLED, o.getStatus());
        assertThrows(BusinessRuleException.class, o::reopen);
    }

    @Test
    void draftCanBeCancelledWithoutReason() {
        Order o = order("DH-1", cashier);
        o.cancel(null, cashier);
        assertEquals(OrderStatus.CANCELLED, o.getStatus());
    }

    @Test
    void paidOrderCannotBeCancelledOrEdited_BR14() {
        Order o = pendingOrder(cashier);
        o.markPaid(null);
        assertEquals("BR14", assertThrows(BusinessRuleException.class, () -> o.cancel("x", cashier)).getRule());
        assertThrows(BusinessRuleException.class, o::reopen);
        assertThrows(BusinessRuleException.class, () -> o.removeItem(1L));
    }

    @Test
    void markPaidOnlyFromPendingPayment_andLinksBuyer_BR22_BR16() {
        Order draft = order("DH-1", cashier);
        assertThrows(BusinessRuleException.class, () -> draft.markPaid(null));
        Order o = pendingOrder(cashier);
        User sv = student(7, "SV23010427");
        o.markPaid(sv);
        assertEquals(OrderStatus.PAID, o.getStatus());
        assertSame(sv, o.getCustomer());
        assertNotNull(o.getPaidAt());
    }

    @Test
    void transferReferenceIsOrderCodeWithoutDashes_BR26() {
        assertEquals("DH2611080001", order("DH-261108-0001", cashier).transferReference());
    }
}
