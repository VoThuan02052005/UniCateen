package vn.edu.phenikaa.unicateen.api.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import vn.edu.phenikaa.unicateen.application.account.AccountService;
import vn.edu.phenikaa.unicateen.application.account.AccountService.AccountView;
import vn.edu.phenikaa.unicateen.application.account.AuthService;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;

/** UC5.1 Dang nhap, dang xuat (cau hinh o SecurityConfig), doi mat khau. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final AccountService accounts;
    private final SessionAuth session;

    public AuthController(AuthService auth, AccountService accounts, SessionAuth session) {
        this.auth = auth;
        this.accounts = accounts;
        this.session = session;
    }

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {
    }

    public record ChangePasswordRequest(@NotBlank String oldPassword, @NotBlank String newPassword) {
    }

    @PostMapping("/login")
    public AccountView login(@Valid @RequestBody LoginRequest body, HttpServletRequest req,
                             HttpServletResponse res) {
        var result = auth.login(body.email(), body.password());
        session.signIn(result.user(), req, res);
        return accounts.me(result.user());
    }

    @GetMapping("/me")
    public AccountView me(@AuthenticationPrincipal CurrentUser me) {
        return accounts.me(me);
    }

    @PostMapping("/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest body,
                                               @AuthenticationPrincipal CurrentUser me) {
        auth.changePassword(me, body.oldPassword(), body.newPassword());
        return ResponseEntity.noContent().build();
    }
}
