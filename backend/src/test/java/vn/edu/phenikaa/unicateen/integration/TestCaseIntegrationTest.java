package vn.edu.phenikaa.unicateen.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import vn.edu.phenikaa.unicateen.application.account.AccountService;
import vn.edu.phenikaa.unicateen.application.catalog.CategoryService;
import vn.edu.phenikaa.unicateen.application.catalog.MenuItemService;
import vn.edu.phenikaa.unicateen.application.catalog.PriceService;
import vn.edu.phenikaa.unicateen.application.catalog.RecipeService;
import vn.edu.phenikaa.unicateen.application.catalog.RecipeService.RecipeLine;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;
import vn.edu.phenikaa.unicateen.application.inventory.InventoryService;
import vn.edu.phenikaa.unicateen.application.inventory.InventoryService.PlanInput;
import vn.edu.phenikaa.unicateen.application.inventory.InventoryService.ReceiptLineInput;
import vn.edu.phenikaa.unicateen.application.payment.PaymentService;
import vn.edu.phenikaa.unicateen.application.report.RevenueReportService;
import vn.edu.phenikaa.unicateen.application.report.RevenueReportService.RevenueReport;
import vn.edu.phenikaa.unicateen.application.sales.OrderService;
import vn.edu.phenikaa.unicateen.application.sales.OrderService.LineInput;
import vn.edu.phenikaa.unicateen.application.sales.OrderView;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.domain.enums.ActiveStatus;
import vn.edu.phenikaa.unicateen.domain.enums.DocumentStatus;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentMethod;
import vn.edu.phenikaa.unicateen.domain.enums.PriceStatus;
import vn.edu.phenikaa.unicateen.domain.enums.RecipeStatus;
import vn.edu.phenikaa.unicateen.domain.enums.Role;
import vn.edu.phenikaa.unicateen.domain.enums.Shift;

/**
 * Hien thuc tu dong cac ca kiem thu thiet ke o Chuong 4 cua bao cao. Ten phuong thuc = ma ca (TC-...),
 * de bang ket qua trong bao cao truy vet duoc toi tung test.
 */
@AutoConfigureMockMvc
class TestCaseIntegrationTest extends IntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired Environment env;
    @Autowired CategoryService categories;
    @Autowired MenuItemService items;
    @Autowired PriceService prices;
    @Autowired RecipeService recipes;
    @Autowired OrderService orders;
    @Autowired PaymentService payments;
    @Autowired InventoryService inventory;
    @Autowired RevenueReportService revenue;
    @Autowired AccountService accounts;

    private static String key() {
        return UUID.randomUUID().toString();
    }

    private String pendingOrder(long menuItemId, int qty) {
        String code = orders.create(List.of(new LineInput(menuItemId, qty)), CASHIER).code();
        orders.submit(code, CASHIER);
        return code;
    }

    private BigDecimal stockOf(long ingredientId) {
        BigDecimal v = jdbc.queryForObject(
                "SELECT COALESCE(SUM(remaining_qty), 0) FROM stock_lots WHERE ingredient_id = ?", BigDecimal.class,
                ingredientId);
        return v == null ? BigDecimal.ZERO : v;
    }

    // ------------------------------------------------------------------ nhom chuc nang

    @Test
    void TC_CAT_01_themDanhMucHopLe() {
        var c = categories.create("Bún -- phở", 2, null);
        assertEquals(ActiveStatus.ACTIVE, c.status());
        assertEquals(2, c.displayOrder());
        assertTrue(categories.list().stream().anyMatch(x -> x.id().equals(c.id())));
    }

    @Test
    void TC_CAT_02_themDanhMucTrungTen() {
        categories.create("Món chay", 5, null);
        int before = count("SELECT COUNT(*) FROM categories");
        BusinessRuleException e = assertThrows(BusinessRuleException.class,
                () -> categories.create("Món chay", 6, null));
        assertEquals("BR01", e.getRule());
        assertEquals("Tên danh mục đã tồn tại", e.getMessage());
        assertEquals(before, count("SELECT COUNT(*) FROM categories"));
    }

    @Test
    void TC_MENU_01_themMonVoiGiaBanDau() {
        var m = items.create(1L, "Cơm gà xối mỡ đặc biệt", "phần", null, null, 35_000, MANAGER);
        var history = prices.history(m.id());
        assertEquals(1, history.size());
        assertEquals(35_000, history.get(0).price());
        assertEquals(PriceStatus.ACTIVE, history.get(0).status());
        assertEquals(35_000L, prices.currentPrice(m.id()));
    }

    @Test
    void TC_PRICE_01_doiGiaKhongAnhHuongDonCu() {
        var m = items.create(1L, "Cơm gà xối mỡ TC-PRICE", "phần", null, null, 35_000, MANAGER);
        OrderView a = orders.create(List.of(new LineInput(m.id(), 1)), CASHIER);
        prices.changePrice(m.id(), 38_000, null, "Tăng giá nguyên liệu", MANAGER);
        OrderView b = orders.create(List.of(new LineInput(m.id(), 1)), CASHIER);
        assertEquals(35_000, orders.get(a.code(), CASHIER).lines().get(0).unitPrice());
        assertEquals(38_000, b.lines().get(0).unitPrice());
        String after = jdbc.queryForObject("SELECT after_value FROM audit_logs WHERE action = 'PRICE_CHANGE' "
                + "AND entity_id = ? ORDER BY id DESC LIMIT 1", String.class, m.id());
        String before = jdbc.queryForObject("SELECT before_value FROM audit_logs WHERE action = 'PRICE_CHANGE' "
                + "AND entity_id = ? ORDER BY id DESC LIMIT 1", String.class, m.id());
        assertTrue(after.contains("38000"));
        assertTrue(before.contains("35000"));
    }

    @Test
    void TC_CAT_03_moiMonMotCongThucDangApDung() {
        recipes.saveNewVersion(1L, "Phiên bản 2", List.of(new RecipeLine(1L, new BigDecimal("0.150")),
                new RecipeLine(2L, new BigDecimal("0.220"))), true);
        var versions = recipes.versions(1L);
        assertEquals(RecipeStatus.ACTIVE, versions.get(0).status());
        assertTrue(versions.stream().skip(1).allMatch(v -> v.status() == RecipeStatus.RETIRED));
        assertEquals(1, count("SELECT COUNT(*) FROM recipes WHERE menu_item_id = 1 AND status = 'ACTIVE'"));
    }

    @Test
    void TC_STK_01_xacNhanPhieuNhap() {
        BigDecimal pho = stockOf(4), bo = stockOf(5);
        var r = inventory.createReceipt(1L, Clock.today(), List.of(
                new ReceiptLineInput(4L, new BigDecimal("6.000"), 20_000, Clock.today().plusDays(2)),
                new ReceiptLineInput(5L, new BigDecimal("3.500"), 250_000, Clock.today().plusDays(4))), INVENTORY);
        var confirmed = inventory.confirmReceipt(r.id(), INVENTORY);
        assertEquals(DocumentStatus.CONFIRMED, confirmed.status());
        assertEquals(2, count("SELECT COUNT(*) FROM stock_lots l JOIN stock_receipt_details d "
                + "ON d.id = l.receipt_detail_id WHERE d.receipt_id = ?", r.id()));
        assertEquals(2, count("SELECT COUNT(*) FROM inventory_transactions WHERE ref_type = 'RECEIPT' "
                + "AND tx_type = 'IN' AND ref_id = ?", r.id()));
        assertEquals(0, pho.add(new BigDecimal("6")).compareTo(stockOf(4)));
        assertEquals(0, bo.add(new BigDecimal("3.5")).compareTo(stockOf(5)));
    }

    @Test
    void TC_STK_02_goiYXuatTheoFefo() {
        LocalDate today = Clock.today();
        var r = inventory.createReceipt(2L, today.minusDays(10), List.of(
                new ReceiptLineInput(7L, new BigDecimal("0.100"), 200_000, today.minusDays(1)),   // lo qua han
                new ReceiptLineInput(7L, new BigDecimal("0.100"), 200_000, today.plusDays(5)),
                new ReceiptLineInput(7L, new BigDecimal("0.100"), 200_000, today.plusDays(2))), INVENTORY);
        inventory.confirmReceipt(r.id(), INVENTORY);
        recipes.saveNewVersion(7L, null, List.of(new RecipeLine(7L, new BigDecimal("0.020"))), true);
        var issue = inventory.createIssue(today, Shift.LUNCH, List.of(new PlanInput(7L, 10)), INVENTORY);
        var lines = inventory.suggestIssue(issue.id()).lines().stream().filter(l -> l.ingredientId() == 7L).toList();
        assertTrue(lines.stream().noneMatch(l -> l.expiryDate().isBefore(today)), "lo qua han bi loai");
        assertEquals(today.plusDays(2), lines.get(0).expiryDate(), "lo het han som hon de xuat truoc");
        BigDecimal total = lines.stream().map(l -> l.suggestedQty()).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("0.200").compareTo(total), "10 phan x 0,020 kg");
    }

    @Test
    void TC_RPT_01_doanhThuChiTinhGiaoDichThanhCong() {
        LocalDate today = Clock.today();
        RevenueReport before = revenue.revenue(today, today);
        String o1 = pendingOrder(1L, 2);
        payments.payCash(o1, 70_000, null, key(), CASHIER);
        String o2 = pendingOrder(1L, 2);
        assertEquals("FAILED", payments.payCard(o2, "SV23010062DECLINE", key(), CASHIER).status().name());
        var tr = payments.startTransfer(o2, null, key(), CASHIER);
        payments.confirmTransfer(tr.code(), CASHIER);
        RevenueReport after = revenue.revenue(today, today);
        assertEquals(140_000, after.totalRevenue() - before.totalRevenue());
        assertEquals(70_000, amount(after, "Tiền mặt") - amount(before, "Tiền mặt"));
        assertEquals(70_000, amount(after, "Chuyển khoản") - amount(before, "Chuyển khoản"));
        assertEquals(0, amount(after, "Thẻ SV/GV") - amount(before, "Thẻ SV/GV"));
        Long sql = jdbc.queryForObject("SELECT COALESCE(SUM(amount),0) FROM payments WHERE payment_status = 'SUCCESS' "
                + "AND DATE(transaction_time) = ?", Long.class, today);
        assertEquals(sql.longValue(), after.totalRevenue());
    }

    private static long amount(RevenueReport r, String method) {
        return r.byMethod().stream().filter(a -> a.key().equals(method)).mapToLong(a -> a.revenue()).sum();
    }

    @Test
    void TC_PAY_08_tienMatCoQuetTheHienTrongLichSu() {
        String code = pendingOrder(1L, 1);
        var p = payments.payCash(code, 50_000, "SV23010320", key(), CASHIER);
        assertEquals("SUCCESS", p.status().name());
        assertNotNull(p.payerName());
        CurrentUser sv = new CurrentUser(8L, "23010320@st.phenikaa-uni.edu.vn", "SV", Role.STUDENT);
        var mine = orders.myHistory(sv).stream().filter(o -> o.code().equals(code)).findFirst().orElseThrow();
        assertEquals(PaymentMethod.CASH, mine.paymentMethod());
    }

    // ------------------------------------------------------------------ nghiep vu va trang thai

    @Test
    void TC_ST_01_thanhToanDonChuaChot() {
        String code = orders.create(List.of(new LineInput(1L, 1)), CASHIER).code();
        assertEquals("BR19", assertThrows(BusinessRuleException.class,
                () -> payments.payCash(code, 50_000, null, key(), CASHIER)).getRule());
        assertEquals(0, count("SELECT COUNT(*) FROM payments p JOIN orders o ON o.id = p.order_id WHERE o.code = ?",
                code));
    }

    @Test
    void TC_ST_05_huyDonDaThanhToanQuaApi() throws Exception {
        String code = pendingOrder(1L, 1);
        payments.payCash(code, 35_000, null, key(), CASHIER);
        mvc.perform(post("/api/orders/" + code + "/cancel").with(as(CASHIER))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"thử\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BR14"));
        assertEquals("PAID", orderStatus(code));
    }

    @Test
    void TC_ST_10_tonKhoKhongDoiKhiBanHang() {
        BigDecimal gao = stockOf(1), ga = stockOf(2);
        String code = pendingOrder(1L, 3);
        payments.payCash(code, 200_000, null, key(), CASHIER);
        assertEquals("PAID", orderStatus(code));
        assertEquals(0, gao.compareTo(stockOf(1)));
        assertEquals(0, ga.compareTo(stockOf(2)));
    }

    // ------------------------------------------------------------------ phan quyen

    @Test
    void TC_AUTH_01_dangNhapDung() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"banhang1@phenikaa-uni.edu.vn\",\"password\":\"Demo@2026\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CASHIER"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) r.getRequest().getSession(false);
        assertNotNull(session);
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk());
        mvc.perform(get("/api/catalog/menu-items/on-sale").session(session)).andExpect(status().isOk());
    }

    @Test
    void TC_AUTH_03_phienHetHanSau30Phut() {
        assertEquals("30m", env.getProperty("server.servlet.session.timeout"));
    }

    @Test
    void TC_AUTH_04_matKhauKhongLuuBanRo() {
        var created = accounts.create("tc-auth-04@phenikaa-uni.edu.vn", "Tài khoản kiểm thử", null, Role.CASHIER, null);
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE id = ?", String.class,
                created.account().id());
        assertTrue(hash.startsWith("$2"), "bcrypt");
        assertEquals(60, hash.length());
        assertNotEquals(created.temporaryPassword(), hash);
        assertFalse(hash.contains(created.temporaryPassword()));
    }

    @Test
    void TC_AUTH_06_sinhVienXemDonNguoiKhac() throws Exception {
        String code = pendingOrder(1L, 1);
        mvc.perform(get("/api/orders/" + code).with(as(STUDENT))).andExpect(status().isForbidden());
    }

    @Test
    void TC_AUTH_07_quanTriVienTuKhoaMinh() {
        assertEquals("BR44", assertThrows(BusinessRuleException.class, () -> accounts.lock(1L, ADMIN)).getRule());
        assertEquals("ACTIVE", jdbc.queryForObject("SELECT status FROM users WHERE id = 1", String.class));
    }

    @Test
    void TC_AUTH_08_nhanVienCheBienSuaMon() throws Exception {
        mvc.perform(put("/api/catalog/menu-items/6").with(as(KITCHEN)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"categoryId\":3,\"name\":\"Đổi tên\",\"unit\":\"chai\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/catalog/menu-items/6/prices").with(as(KITCHEN)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"price\":1000}")).andExpect(status().isForbidden());
        mvc.perform(patch("/api/catalog/menu-items/6/status").with(as(KITCHEN)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"OUT_OF_STOCK\"}")).andExpect(status().isOk());
        mvc.perform(patch("/api/catalog/menu-items/6/status").with(as(KITCHEN)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ON_SALE\"}")).andExpect(status().isOk());
        assertEquals("Nước suối 500ml", jdbc.queryForObject("SELECT name FROM menu_items WHERE id = 6", String.class));
    }

    @Test
    void TC_AUTH_09_doiVaiTroTaiKhoan() throws Exception {
        var created = accounts.create("tc-auth-09@phenikaa-uni.edu.vn", "Nhân viên đổi vai trò", null, Role.CASHIER, null);
        Long id = created.account().id();
        accounts.changeRole(id, Role.INVENTORY_STAFF, ADMIN);
        String before = jdbc.queryForObject("SELECT before_value FROM audit_logs WHERE action = 'ROLE_CHANGE' "
                + "AND entity_id = ? ORDER BY id DESC LIMIT 1", String.class, id);
        String after = jdbc.queryForObject("SELECT after_value FROM audit_logs WHERE action = 'ROLE_CHANGE' "
                + "AND entity_id = ? ORDER BY id DESC LIMIT 1", String.class, id);
        assertTrue(before.contains("CASHIER"));
        assertTrue(after.contains("INVENTORY_STAFF"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"tc-auth-09@phenikaa-uni.edu.vn\",\"password\":\""
                                + created.temporaryPassword() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("INVENTORY_STAFF"));
        CurrentUser u = new CurrentUser(id, "tc-auth-09@phenikaa-uni.edu.vn", "NV", Role.INVENTORY_STAFF);
        mvc.perform(get("/api/stock/levels").with(as(u))).andExpect(status().isOk());
        mvc.perform(get("/api/orders").with(as(u))).andExpect(status().isForbidden());
    }

    @Test
    void TC_AUTH_11_chiLuuDuLieuCaNhanCanThiet() {
        String guest = pendingOrder(5L, 1);
        payments.payCash(guest, 10_000, null, key(), CASHIER);
        assertNull(jdbc.queryForObject("SELECT customer_id FROM orders WHERE code = ?", Long.class, guest));
        var result = accounts.importCsv("email,full_name,role,card_code,birth_date,address\n"
                + "tc-auth-11@st.phenikaa-uni.edu.vn,Sinh Viên Nhập Tệp,STUDENT,SVTC11,2005-01-01,Hà Nội\n");
        assertEquals(1, result.created());
        assertTrue(result.errors().isEmpty());
        assertEquals("SVTC11", jdbc.queryForObject("SELECT card_code FROM users WHERE email = ?", String.class,
                "tc-auth-11@st.phenikaa-uni.edu.vn"));
        assertEquals(0, count("SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() "
                + "AND table_name = 'users' AND column_name IN ('birth_date', 'address', 'date_of_birth')"));
    }
}
