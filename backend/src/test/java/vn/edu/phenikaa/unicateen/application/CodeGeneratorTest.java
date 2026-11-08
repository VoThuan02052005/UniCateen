package vn.edu.phenikaa.unicateen.application;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import vn.edu.phenikaa.unicateen.application.common.CodeGenerator;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.support.Fixtures;

/** Sinh ma chung tu theo DV08, BR10. */
class CodeGeneratorTest {
    @AfterEach
    void reset() {
        Clock.reset();
    }

    @Test
    void codeFollowsPrefixDateSequence_DV08() {
        Fixtures.freezeClock(LocalDateTime.of(2026, 11, 8, 10, 0));
        CodeGenerator g = new CodeGenerator();
        assertEquals("DH-261108-0001", g.next("DH", 4, p -> null));
        assertEquals("DH-261108-0002", g.next("DH", 4, p -> null));          // nho so da cap trong tien trinh
        assertEquals("DH-261108-0008", g.next("DH", 4, p -> p + "0007"));    // lay theo CSDL neu lon hon
        assertEquals("PN-261108-001", g.next("PN", 3, p -> null));
    }

    @Test
    void sequentialCodesUseHighestExisting() {
        assertEquals("DM03", CodeGenerator.sequential("DM", 2, List.of("DM01", "DM02")));
        assertEquals("M011", CodeGenerator.sequential("M", 3, List.of("M001", "M010", "X999")));
        assertEquals("NCC001", CodeGenerator.sequential("NCC", 3, List.of()));
    }
}
