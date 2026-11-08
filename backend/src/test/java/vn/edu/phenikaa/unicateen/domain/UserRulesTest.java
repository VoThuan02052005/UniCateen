package vn.edu.phenikaa.unicateen.domain;

import static org.junit.jupiter.api.Assertions.*;
import static vn.edu.phenikaa.unicateen.support.Fixtures.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import vn.edu.phenikaa.unicateen.domain.enums.Role;
import vn.edu.phenikaa.unicateen.domain.model.ItemPrice;
import vn.edu.phenikaa.unicateen.domain.model.User;

/** Tai khoan va gia (TC-AUTH, TC-CAT): BR02, BR05, BR43, BR45. */
class UserRulesTest {

    @Test
    void fiveWrongPasswordsLockFor15Minutes_BR43() {
        User u = user(3, Role.CASHIER);
        LocalDateTime t = LocalDateTime.of(2026, 11, 8, 9, 0);
        for (int i = 0; i < 4; i++) {
            u.recordFailedLogin(5, 15, t);
        }
        assertFalse(u.isTemporarilyLocked(t));
        u.recordFailedLogin(5, 15, t);
        assertTrue(u.isTemporarilyLocked(t.plusMinutes(14)));
        assertFalse(u.isTemporarilyLocked(t.plusMinutes(15)));
        u.recordSuccessfulLogin(t.plusMinutes(16));
        assertEquals(0, u.getFailedAttempts());
    }

    @Test
    void cardOnlyForStudentOrFaculty_BR45() {
        assertEquals("BR45", assertThrows(BusinessRuleException.class,
                () -> user(3, Role.CASHIER).linkCard("SV1")).getRule());
        User sv = student(7, "SV23010427");
        assertEquals("SV23010427", sv.getCardCode());
        assertThrows(BusinessRuleException.class, () -> sv.changeRole(Role.CASHIER));
        sv.changeRole(Role.FACULTY);
        assertEquals(Role.FACULTY, sv.getRole());
    }

    @Test
    void priceMustBePositive_andEffectiveWindow_BR02_BR05() {
        User ql = user(2, Role.MANAGER);
        LocalDateTime now = LocalDateTime.of(2026, 11, 8, 9, 0);
        assertThrows(BusinessRuleException.class, () -> new ItemPrice(item(1, "Cơm"), 0, now, null, ql, now));
        ItemPrice p = new ItemPrice(item(1, "Cơm"), 35_000, now, null, ql, now);
        assertTrue(p.isEffectiveAt(now));
        p.closeAt(now.plusDays(1));
        assertFalse(p.isEffectiveAt(now.plusDays(1)));
        ItemPrice later = new ItemPrice(item(1, "Cơm"), 38_000, now.plusDays(1), null, ql, now);
        assertTrue(later.isEffectiveAt(now.plusDays(1)));
        assertFalse(later.isEffectiveAt(now));
    }
}
