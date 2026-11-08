package vn.edu.phenikaa.unicateen.infrastructure.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import vn.edu.phenikaa.unicateen.application.port.IPasswordHasher;

/**
 * Chi chay voi profile "demo": dat mat khau "Demo@2026" (bam bcrypt) cho cac tai khoan mau
 * co password_hash = '{demo}' trong V3__seed_demo.sql. Khong bao gio luu mat khau dang ro.
 */
@Component
@Profile("demo")
public class DemoPasswordInitializer implements ApplicationRunner {
    public static final String DEMO_PASSWORD = "Demo@2026";
    private static final Logger log = LoggerFactory.getLogger(DemoPasswordInitializer.class);

    private final JdbcTemplate jdbc;
    private final IPasswordHasher hasher;

    public DemoPasswordInitializer(JdbcTemplate jdbc, IPasswordHasher hasher) {
        this.jdbc = jdbc;
        this.hasher = hasher;
    }

    @Override
    public void run(ApplicationArguments args) {
        int n = jdbc.update("UPDATE users SET password_hash = ? WHERE password_hash = '{demo}'",
                hasher.hash(DEMO_PASSWORD));
        if (n > 0) {
            log.info("Da dat mat khau demo cho {} tai khoan mau", n);
        }
    }
}
