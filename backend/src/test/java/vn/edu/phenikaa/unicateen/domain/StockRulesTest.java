package vn.edu.phenikaa.unicateen.domain;

import static org.junit.jupiter.api.Assertions.*;
import static vn.edu.phenikaa.unicateen.support.Fixtures.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import vn.edu.phenikaa.unicateen.domain.enums.ActiveStatus;
import vn.edu.phenikaa.unicateen.domain.enums.DocumentStatus;
import vn.edu.phenikaa.unicateen.domain.enums.Role;
import vn.edu.phenikaa.unicateen.domain.enums.TxType;
import vn.edu.phenikaa.unicateen.domain.model.*;

/** Quy tac kho (TC-STK): BR31-BR37, GD6. */
class StockRulesTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 8);
    private final User kho = user(5, Role.INVENTORY_STAFF);
    private final User quanLy = user(2, Role.MANAGER);

    @Test
    void receiptLineValidation_BR32() {
        StockReceipt r = new StockReceipt("PN-1", supplier(), TODAY, kho);
        Ingredient ga = ingredient(2, "Đùi gà", true);
        assertThrows(BusinessRuleException.class, () -> r.addLine(ga, BigDecimal.ZERO, 75_000, TODAY.plusDays(3)));
        assertThrows(BusinessRuleException.class, () -> r.addLine(ga, BigDecimal.ONE, -1, TODAY.plusDays(3)));
        assertThrows(BusinessRuleException.class, () -> r.addLine(ga, BigDecimal.ONE, 75_000, null));
        assertThrows(BusinessRuleException.class, () -> r.addLine(ga, BigDecimal.ONE, 75_000, TODAY.minusDays(1)));
        r.addLine(ga, new BigDecimal("2.5"), 75_000, TODAY.plusDays(3));
        r.addLine(ingredient(1, "Gạo", false), new BigDecimal("50"), 18_000, null);
        assertEquals(187_500 + 900_000, r.getTotalValue());
    }

    @Test
    void inactiveSupplierCannotReceive_BR30() {
        Supplier s = supplier();
        s.setStatus(ActiveStatus.INACTIVE);
        assertThrows(BusinessRuleException.class, () -> new StockReceipt("PN-1", s, TODAY, kho));
    }

    @Test
    void confirmedReceiptIsImmutable_BR31() {
        StockReceipt r = new StockReceipt("PN-1", supplier(), TODAY, kho);
        assertThrows(BusinessRuleException.class, r::confirm);          // phieu rong
        r.addLine(ingredient(1, "Gạo", false), BigDecimal.TEN, 18_000, null);
        r.confirm();
        assertEquals(DocumentStatus.CONFIRMED, r.getStatus());
        assertEquals("BR31", assertThrows(BusinessRuleException.class, r::confirm).getRule());
        assertThrows(BusinessRuleException.class, r::clearLines);
        assertThrows(BusinessRuleException.class, r::cancel);
    }

    @Test
    void lotFromReceiptLineKeepsQuantityAndExpiry_GD6() {
        StockLot lot = lot(1, ingredient(2, "Đùi gà", true), "30", TODAY.plusDays(2), TODAY);
        assertEquals(new BigDecimal("30"), lot.getRemainingQty());
        assertEquals(TODAY.plusDays(2), lot.getExpiryDate());
    }

    @Test
    void cannotIssueExpiredLotOrMoreThanRemaining_BR34_BR35() {
        StockLot expired = lot(1, ingredient(2, "Đùi gà", true), "5", TODAY.minusDays(1), TODAY.minusDays(5));
        assertEquals("BR34", assertThrows(BusinessRuleException.class,
                () -> expired.deduct(BigDecimal.ONE, TODAY)).getRule());
        StockLot ok = lot(2, ingredient(2, "Đùi gà", true), "5", TODAY.plusDays(3), TODAY);
        assertEquals("BR35", assertThrows(BusinessRuleException.class,
                () -> ok.deduct(new BigDecimal("5.001"), TODAY)).getRule());
        ok.deduct(new BigDecimal("5"), TODAY);
        assertEquals(0, ok.getRemainingQty().signum());
    }

    @Test
    void nearExpiryWarningWithinThreeDays_BR38() {
        Ingredient ga = ingredient(2, "Đùi gà", true);
        assertTrue(lot(1, ga, "1", TODAY.plusDays(3), TODAY).isNearExpiry(TODAY, 3));
        assertFalse(lot(2, ga, "1", TODAY.plusDays(4), TODAY).isNearExpiry(TODAY, 3));
        assertFalse(lot(3, ga, "1", TODAY.minusDays(1), TODAY.minusDays(9)).isNearExpiry(TODAY, 3));
    }

    @Test
    void adjustmentNeedsReason_recordsBeforeAfter_andSignedTransaction_BR37() {
        StockLot l = lot(1, ingredient(2, "Đùi gà", true), "5.000", TODAY.plusDays(3), TODAY);
        assertThrows(BusinessRuleException.class,
                () -> StockAdjustment.apply("DC-1", l, new BigDecimal("4.6"), " ", quanLy));
        assertThrows(BusinessRuleException.class,
                () -> StockAdjustment.apply("DC-1", l, new BigDecimal("-1"), "kiểm kê", quanLy));
        StockAdjustment a = withId(StockAdjustment.apply("DC-1", l, new BigDecimal("4.600"), "hao hụt", quanLy), 9);
        assertEquals(new BigDecimal("5.000"), a.getQtyBefore());
        assertEquals(new BigDecimal("4.600"), l.getRemainingQty());
        InventoryTransaction tx = InventoryTransaction.adjust(a, quanLy);
        assertEquals(TxType.ADJUST, tx.getType());
        assertEquals(new BigDecimal("-0.400"), tx.getQuantity());
        assertEquals("hao hụt", tx.getReason());
    }

    @Test
    void outTransactionIsNegative_inIsPositive_BR33() {
        StockLot l = lot(1, ingredient(1, "Gạo", false), "10", null, TODAY);
        assertEquals(-1, InventoryTransaction.out(l, new BigDecimal("2"), 1L, kho).getQuantity().signum());
        assertEquals(1, InventoryTransaction.in(l, new BigDecimal("2"), 1L, kho).getQuantity().signum());
    }

    @Test
    void recipeRejectsDuplicateIngredientAndNonPositiveQty_BR08() {
        Recipe r = new Recipe(item(1, "Cơm gà"), 1, null);
        Ingredient gao = ingredient(1, "Gạo", false);
        assertThrows(BusinessRuleException.class, () -> r.addDetail(gao, BigDecimal.ZERO));
        r.addDetail(gao, new BigDecimal("0.15"));
        assertThrows(BusinessRuleException.class, () -> r.addDetail(gao, new BigDecimal("0.1")));
        r.activate();
        assertTrue(r.isActive());
    }

    @Test
    void ingredientUnitFixedOnceTransactionsExist_BR07() {
        Ingredient gao = ingredient(1, "Gạo", false);
        gao.changeUnit("bao", false);
        assertEquals("bao", gao.getUnit());
        assertEquals("BR07", assertThrows(BusinessRuleException.class, () -> gao.changeUnit("kg", true)).getRule());
    }
}
