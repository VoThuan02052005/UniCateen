package vn.edu.phenikaa.unicateen.support;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.domain.enums.Role;
import vn.edu.phenikaa.unicateen.domain.model.*;

/** Tao entity cho unit test domain (khong can CSDL); id duoc gan bang phan chieu. */
public final class Fixtures {
    private Fixtures() {
    }

    public static <T> T withId(T entity, long id) {
        try {
            Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public static User user(long id, Role role) {
        return withId(User.create("u" + id + "@phenikaa-uni.edu.vn", "Người dùng " + id, null, role, "hash"), id);
    }

    public static User student(long id, String card) {
        User u = user(id, Role.STUDENT);
        u.linkCard(card);
        return u;
    }

    public static MenuItem item(long id, String name) {
        Category c = withId(new Category("DM01", "Cơm", 1, null), 1);
        return withId(new MenuItem("M" + id, c, name, "phần", null, null), id);
    }

    public static Order order(String code, User cashier) {
        return withId(new Order(code, cashier), 1);
    }

    /** Don PENDING_PAYMENT tong 70.000 d (2 x 35.000). */
    public static Order pendingOrder(User cashier) {
        Order o = order("DH-261108-0001", cashier);
        o.addItem(item(1, "Cơm gà"), 2, 35_000);
        o.submit();
        return o;
    }

    public static Ingredient ingredient(long id, String name, boolean trackExpiry) {
        return withId(new Ingredient("NL" + id, name, "Thịt, cá", "kg", new BigDecimal("5"), trackExpiry, null), id);
    }

    public static Supplier supplier() {
        return withId(new Supplier("NCC001", "NCC Hà Đông", null, null, "0987000111", null, null, null), 1);
    }

    /** Lo tu mot phieu nhap da xac nhan. */
    public static StockLot lot(long id, Ingredient ing, String qty, LocalDate expiry, LocalDate received) {
        StockReceipt r = withId(new StockReceipt("PN-1", supplier(), received, user(5, Role.INVENTORY_STAFF)), id);
        r.addLine(ing, new BigDecimal(qty), 10_000, expiry);
        return withId(StockLot.fromReceiptLine("L-" + id, r.getDetails().get(0)), id);
    }

    public static void freezeClock(LocalDateTime t) {
        Clock.set(java.time.Clock.fixed(t.atZone(Clock.ZONE).toInstant(), Clock.ZONE));
    }
}
