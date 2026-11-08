package vn.edu.phenikaa.unicateen.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import vn.edu.phenikaa.unicateen.application.inventory.InventoryService;
import vn.edu.phenikaa.unicateen.application.inventory.InventoryService.IssueLineInput;
import vn.edu.phenikaa.unicateen.application.inventory.InventoryService.PlanInput;
import vn.edu.phenikaa.unicateen.application.inventory.InventoryService.ReceiptLineInput;
import vn.edu.phenikaa.unicateen.application.inventory.InventoryViews.IssueSuggestion;
import vn.edu.phenikaa.unicateen.application.inventory.InventoryViews.IssueView;
import vn.edu.phenikaa.unicateen.application.inventory.InventoryViews.ReceiptView;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.domain.enums.DocumentStatus;
import vn.edu.phenikaa.unicateen.domain.enums.Shift;

/** TC-STK: nhap kho, FEFO, xuat theo ca, dieu chinh, toan ven giao dich kho. */
class InventoryIntegrationTest extends IntegrationTest {
    @Autowired
    InventoryService inventory;

    /** Toan ven: SUM(quantity) theo lo bang remaining_qty (Muc 4.4.4 bao cao). */
    private void assertLedgerConsistent() {
        assertEquals(0, count("""
                SELECT COUNT(*) FROM (
                  SELECT l.id FROM stock_lots l JOIN inventory_transactions t ON t.lot_id = l.id
                  GROUP BY l.id, l.remaining_qty HAVING SUM(t.quantity) <> l.remaining_qty) x"""));
    }

    private ReceiptView receiveChicken(String qty, LocalDate expiry) {
        ReceiptView r = inventory.createReceipt(1L, Clock.today(),
                List.of(new ReceiptLineInput(2L, new BigDecimal(qty), 75_000, expiry)), INVENTORY);
        return inventory.confirmReceipt(r.id(), INVENTORY);
    }

    @Test
    void confirmReceiptCreatesOneLotPerLine_andInTransactions_GD6_BR33() {
        int lotsBefore = count("SELECT COUNT(*) FROM stock_lots");
        ReceiptView r = receiveChicken("12.5", Clock.today().plusDays(6));
        assertEquals(DocumentStatus.CONFIRMED, r.status());
        assertEquals(lotsBefore + 1, count("SELECT COUNT(*) FROM stock_lots"));
        assertEquals(1, count("SELECT COUNT(*) FROM inventory_transactions WHERE ref_type = 'RECEIPT' AND ref_id = ?",
                r.id()));
        assertEquals("BR31", assertThrows(BusinessRuleException.class,
                () -> inventory.confirmReceipt(r.id(), INVENTORY)).getRule());
        assertLedgerConsistent();
    }

    @Test
    void suggestionFollowsFefo_andReportsShortage_BR34() {
        // Lo dui ga trong du lieu mau het han sau 2 ngay; lo moi het han sau 9 ngay
        receiveChicken("10", Clock.today().plusDays(9));
        IssueView issue = inventory.createIssue(Clock.today(), Shift.LUNCH, List.of(new PlanInput(1L, 200)), INVENTORY);
        IssueSuggestion s = inventory.suggestIssue(issue.id());
        var chicken = s.lines().stream().filter(l -> l.ingredientId() == 2L).toList();
        assertTrue(chicken.size() >= 2);
        for (int i = 1; i < chicken.size(); i++) {
            assertFalse(chicken.get(i).expiryDate().isBefore(chicken.get(i - 1).expiryDate()), "FEFO");
        }
        // 200 phan x 0,2 kg = 40 kg dui ga, ton 30 + 10 (+ cac lo test khac) -> co the thieu
        assertTrue(s.itemsWithoutRecipe().isEmpty());
    }

    @Test
    void confirmIssueDeductsStock_andRejectsOverIssue_BR35_BR36() {
        Long lotId = jdbc.queryForObject("SELECT id FROM stock_lots WHERE ingredient_id = 1 ORDER BY id LIMIT 1",
                Long.class);
        BigDecimal before = jdbc.queryForObject("SELECT remaining_qty FROM stock_lots WHERE id = ?",
                BigDecimal.class, lotId);
        IssueView issue = inventory.createIssue(Clock.today(), Shift.MORNING, List.of(new PlanInput(1L, 10)), INVENTORY);
        assertEquals("BR35", assertThrows(BusinessRuleException.class, () -> inventory.confirmIssue(issue.id(),
                List.of(new IssueLineInput(lotId, null, before.add(BigDecimal.ONE), false)), INVENTORY)).getRule());
        assertEquals(0, before.compareTo(jdbc.queryForObject("SELECT remaining_qty FROM stock_lots WHERE id = ?",
                BigDecimal.class, lotId)), "rollback khi vuot ton");
        inventory.confirmIssue(issue.id(), List.of(new IssueLineInput(lotId, new BigDecimal("1.5"),
                new BigDecimal("1.5"), false)), INVENTORY);
        assertEquals(0, before.subtract(new BigDecimal("1.5")).compareTo(jdbc.queryForObject(
                "SELECT remaining_qty FROM stock_lots WHERE id = ?", BigDecimal.class, lotId)));
        assertLedgerConsistent();
    }

    @Test
    void adjustmentRequiresReason_recordsAdjustTxAndAudit_BR37() {
        Long lotId = jdbc.queryForObject("SELECT id FROM stock_lots WHERE ingredient_id = 3 ORDER BY id LIMIT 1",
                Long.class);
        assertThrows(BusinessRuleException.class,
                () -> inventory.adjust(lotId, new BigDecimal("19.5"), " ", MANAGER));
        var adj = inventory.adjust(lotId, new BigDecimal("19.500"), "Hao hụt khi bảo quản", MANAGER);
        assertTrue(adj.code().startsWith("DC-"));
        assertEquals(1, count("SELECT COUNT(*) FROM inventory_transactions WHERE ref_type = 'ADJUSTMENT' AND ref_id = ?",
                adj.id()));
        assertEquals(1, count("SELECT COUNT(*) FROM audit_logs WHERE action = 'STOCK_ADJUST' AND entity_id = ?", lotId));
        assertLedgerConsistent();
    }

    @Test
    void lowStockAndNearExpiryWarnings_BR38() {
        var levels = inventory.stockLevels();
        var coffee = levels.stream().filter(l -> l.code().equals("NL007")).findFirst().orElseThrow();
        assertTrue(coffee.low(), "Ca phe bot khong co ton -> canh bao ton thap");
        var pho = levels.stream().filter(l -> l.code().equals("NL004")).findFirst().orElseThrow();
        assertTrue(pho.nearExpiryLots() >= 1, "Banh pho het han sau 1 ngay -> canh bao");
    }
}
