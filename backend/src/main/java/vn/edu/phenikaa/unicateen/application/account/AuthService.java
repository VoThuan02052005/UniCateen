package vn.edu.phenikaa.unicateen.application.account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phenikaa.unicateen.application.common.Actors;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;
import vn.edu.phenikaa.unicateen.application.port.IPasswordHasher;
import vn.edu.phenikaa.unicateen.application.port.UserRepository;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.domain.model.User;

/** UC5.1 Dang nhap bang email va mat khau rieng cua ung dung (GD1, BR40, BR41, BR43). */
@Service
public class AuthService {
    private final UserRepository users;
    private final IPasswordHasher hasher;
    private final Actors actors;
    private final int maxAttempts;
    private final int lockMinutes;

    public AuthService(UserRepository users, IPasswordHasher hasher, Actors actors,
                       @Value("${unicateen.login.max-failed-attempts:5}") int maxAttempts,
                       @Value("${unicateen.login.lock-minutes:15}") int lockMinutes) {
        this.users = users;
        this.hasher = hasher;
        this.actors = actors;
        this.maxAttempts = maxAttempts;
        this.lockMinutes = lockMinutes;
    }

    /** Ket qua dang nhap; {@code mustChangePassword} khi dang dung mat khau tam. */
    public record LoginResult(CurrentUser user, boolean mustChangePassword) {
    }

    /** Loi dang nhap dung chung mot thong bao de khong lo email nao ton tai. */
    public static class LoginFailedException extends RuntimeException {
        public LoginFailedException(String message) {
            super(message);
        }
    }

    @Transactional(noRollbackFor = LoginFailedException.class)
    public LoginResult login(String email, String password) {
        var now = Clock.now();
        User u = users.findByEmail(email == null ? "" : email.trim().toLowerCase())
                .orElseThrow(() -> new LoginFailedException("Email hoặc mật khẩu không đúng"));
        if (!u.isActive()) {
            throw new LoginFailedException("Tài khoản đã bị khóa, liên hệ quản trị viên");
        }
        if (u.isTemporarilyLocked(now)) {
            throw new LoginFailedException("Đăng nhập sai quá 5 lần, thử lại sau " + lockMinutes + " phút");
        }
        if (!hasher.matches(password, u.getPasswordHash())) {
            u.recordFailedLogin(maxAttempts, lockMinutes, now);                // BR43
            throw new LoginFailedException("Email hoặc mật khẩu không đúng");
        }
        u.recordSuccessfulLogin(now);
        return new LoginResult(new CurrentUser(u.getId(), u.getEmail(), u.getFullName(), u.getRole()),
                u.isMustChangePassword());
    }

    @Transactional
    public void changePassword(CurrentUser actor, String oldPassword, String newPassword) {
        User u = actors.load(actor);
        if (!hasher.matches(oldPassword, u.getPasswordHash())) {
            throw new BusinessRuleException("BR41", "Mật khẩu hiện tại không đúng");
        }
        PasswordPolicy.check(newPassword);
        u.changePassword(hasher.hash(newPassword));
    }
}
