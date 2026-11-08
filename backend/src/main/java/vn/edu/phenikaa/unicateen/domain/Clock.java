package vn.edu.phenikaa.unicateen.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/** Dong ho dung chung theo mui gio Viet Nam; test co the dat thoi diem co dinh. */
public final class Clock {
    public static final ZoneId ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static java.time.Clock clock = java.time.Clock.system(ZONE);

    private Clock() {
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(clock).withNano(0);
    }

    public static LocalDate today() {
        return LocalDate.now(clock);
    }

    /** Chi dung trong test. */
    public static void set(java.time.Clock fixed) {
        clock = fixed;
    }

    public static void reset() {
        clock = java.time.Clock.system(ZONE);
    }
}
