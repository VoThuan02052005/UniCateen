package vn.edu.phenikaa.unicateen.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;

/** Gan nguoi dung da xac thuc vao phien sau khi AuthService.login thanh cong. */
@Component
public class SessionAuth {
    private final SecurityContextRepository contexts;

    public SessionAuth(SecurityContextRepository contexts) {
        this.contexts = contexts;
    }

    public void signIn(CurrentUser user, HttpServletRequest req, HttpServletResponse res) {
        req.getSession(true);
        req.changeSessionId();                       // chong co dinh phien
        var auth = new UsernamePasswordAuthenticationToken(user, null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
        SecurityContext ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
        contexts.saveContext(ctx, req, res);
    }
}
