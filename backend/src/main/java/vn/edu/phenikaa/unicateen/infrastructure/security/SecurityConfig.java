package vn.edu.phenikaa.unicateen.infrastructure.security;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * Lop kiem tra thu nhat: vai tro theo nhom API (Bang vai tro theo nhom API, Chuong 3; BR42).
 * Lop thu hai (quyen so huu du lieu, BR17) nam trong service.
 * Phien luu o may chu, cookie HttpOnly, SameSite=Strict; het han sau 30 phut (application.yml, BR43).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    private static final String ADMIN = "ADMIN";
    private static final String MANAGER = "MANAGER";
    private static final String CASHIER = "CASHIER";
    private static final String INVENTORY = "INVENTORY_STAFF";
    private static final String KITCHEN = "KITCHEN_STAFF";
    private static final String STUDENT = "STUDENT";
    private static final String FACULTY = "FACULTY";

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, SecurityContextRepository contexts) throws Exception {
        http
            // Cookie phien SameSite=Strict chan yeu cau cheo trang; API chi nhan JSON.
            .csrf(csrf -> csrf.disable())
            .securityContext(sc -> sc.securityContextRepository(contexts))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                    .sessionFixation(f -> f.changeSessionId()))
            .exceptionHandling(e -> e
                    .authenticationEntryPoint((req, res, ex) -> json(res, HttpServletResponse.SC_UNAUTHORIZED,
                            "UNAUTHENTICATED", "Chưa đăng nhập hoặc phiên đã hết hạn"))
                    .accessDeniedHandler((req, res, ex) -> json(res, HttpServletResponse.SC_FORBIDDEN,
                            "FORBIDDEN", "Không có quyền thực hiện chức năng này")))
            .logout(l -> l.logoutUrl("/api/auth/logout")
                    .logoutSuccessHandler((req, res, a) -> res.setStatus(HttpServletResponse.SC_NO_CONTENT)))
            .authorizeHttpRequests(auth -> auth
                    // giao dien tinh
                    .requestMatchers("/", "/index.html", "/assets/**", "/favicon.*").permitAll()
                    // /auth
                    .requestMatchers(POST, "/api/auth/login").permitAll()
                    .requestMatchers("/api/auth/**").authenticated()
                    // /catalog
                    .requestMatchers(GET, "/api/catalog/menu-items/on-sale").hasAnyRole(MANAGER, CASHIER, KITCHEN)
                    .requestMatchers(GET, "/api/catalog/categories/**", "/api/catalog/menu-items/**")
                        .hasAnyRole(MANAGER, CASHIER, KITCHEN, INVENTORY)
                    .requestMatchers(PATCH, "/api/catalog/menu-items/*/status").hasAnyRole(MANAGER, KITCHEN)
                    .requestMatchers(GET, "/api/catalog/ingredients/**").hasAnyRole(MANAGER, INVENTORY, KITCHEN)
                    .requestMatchers(GET, "/api/catalog/recipes/**").hasAnyRole(MANAGER, KITCHEN, INVENTORY)
                    .requestMatchers("/api/catalog/**").hasRole(MANAGER)
                    // /orders
                    .requestMatchers(GET, "/api/orders/mine").hasAnyRole(STUDENT, FACULTY)
                    .requestMatchers("/api/orders/**").hasAnyRole(CASHIER, MANAGER)
                    // /payments
                    .requestMatchers("/api/payments/**").hasAnyRole(CASHIER, MANAGER)
                    // /stock: dieu chinh ton chi MANAGER (BR37)
                    .requestMatchers(POST, "/api/stock/adjustments/**").hasRole(MANAGER)
                    .requestMatchers("/api/stock/**").hasAnyRole(INVENTORY, MANAGER)
                    // /kitchen
                    .requestMatchers("/api/kitchen/**").hasRole(KITCHEN)
                    // /accounts
                    .requestMatchers("/api/accounts/**").hasRole(ADMIN)
                    // /reports
                    .requestMatchers("/api/reports/stock/**").hasAnyRole(MANAGER, INVENTORY)
                    .requestMatchers("/api/reports/**").hasRole(MANAGER)
                    .requestMatchers("/api/**").denyAll()
                    .anyRequest().permitAll());
        return http.build();
    }

    private static void json(HttpServletResponse res, int status, String code, String message)
            throws java.io.IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}");
    }
}
