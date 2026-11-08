package vn.edu.phenikaa.unicateen.application.common;

import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import vn.edu.phenikaa.unicateen.domain.Clock;

/**
 * Sinh ma chung tu dang TIENTO-yymmdd-nnnn (BR10, DV08). Lay ma lon nhat trong CSDL va so da cap trong
 * tien trinh de tranh trung khi hai giao dich chay song song; UNIQUE o CSDL la lop bao ve cuoi.
 */
@Component
public class CodeGenerator {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyMMdd");
    private final Map<String, Integer> lastIssued = new HashMap<>();

    public synchronized String next(String prefix, int width, Function<String, String> maxCodeLookup) {
        String p = prefix + "-" + Clock.today().format(DAY) + "-";
        String max = maxCodeLookup.apply(p);
        int fromDb = max == null ? 0 : Integer.parseInt(max.substring(p.length()));
        int n = Math.max(fromDb, lastIssued.getOrDefault(p, 0)) + 1;
        lastIssued.put(p, n);
        return p + String.format("%0" + width + "d", n);
    }

    /** Ma danh muc dang tien to + so thu tu, vi du DM01, M001, NL001, NCC001; lay so lon nhat da co + 1. */
    public static String sequential(String prefix, int width, Collection<String> existingCodes) {
        int max = existingCodes.stream()
                .filter(c -> c != null && c.startsWith(prefix) && c.substring(prefix.length()).matches("\\d+"))
                .mapToInt(c -> Integer.parseInt(c.substring(prefix.length())))
                .max().orElse(0);
        return prefix + String.format("%0" + width + "d", max + 1);
    }
}
