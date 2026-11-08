package vn.edu.phenikaa.unicateen.integration;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.MySQLContainer;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;
import vn.edu.phenikaa.unicateen.domain.enums.Role;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

/**
 * Lop cha cho test tich hop: MySQL 8 that qua Testcontainers (khong mock CSDL khi kiem tra BR20, UNIQUE),
 * du lieu mau V3__seed_demo.sql (profile demo). Can Docker dang chay.
 * Container dung chung cho moi lop test (khoi dong mot lan) de khop voi Spring context duoc cache.
 */
@SpringBootTest
@ActiveProfiles({"demo", "test"})
public abstract class IntegrationTest {
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withEnv("TZ", "Asia/Ho_Chi_Minh")          // NOW() trong du lieu mau cung mui gio voi ung dung
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

    static {
        MYSQL.start();
    }

    // Tai khoan mau trong V3__seed_demo.sql
    protected static final CurrentUser ADMIN = new CurrentUser(1L, "admin@phenikaa-uni.edu.vn", "Admin", Role.ADMIN);
    protected static final CurrentUser MANAGER = new CurrentUser(2L, "quanly@phenikaa-uni.edu.vn", "QL", Role.MANAGER);
    protected static final CurrentUser CASHIER = new CurrentUser(3L, "banhang1@phenikaa-uni.edu.vn", "BH1", Role.CASHIER);
    protected static final CurrentUser CASHIER2 = new CurrentUser(4L, "banhang2@phenikaa-uni.edu.vn", "BH2", Role.CASHIER);
    protected static final CurrentUser INVENTORY = new CurrentUser(5L, "kho@phenikaa-uni.edu.vn", "Kho", Role.INVENTORY_STAFF);
    protected static final CurrentUser KITCHEN = new CurrentUser(6L, "bep@phenikaa-uni.edu.vn", "Bep", Role.KITCHEN_STAFF);
    protected static final CurrentUser STUDENT = new CurrentUser(7L, "23010427@st.phenikaa-uni.edu.vn", "SV", Role.STUDENT);

    @Autowired
    protected JdbcTemplate jdbc;

    protected static RequestPostProcessor as(CurrentUser u) {
        return authentication(new UsernamePasswordAuthenticationToken(u, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + u.role().name()))));
    }

    protected String orderStatus(String code) {
        return jdbc.queryForObject("SELECT status FROM orders WHERE code = ?", String.class, code);
    }

    protected int count(String sql, Object... args) {
        Integer n = jdbc.queryForObject(sql, Integer.class, args);
        return n == null ? 0 : n;
    }
}
