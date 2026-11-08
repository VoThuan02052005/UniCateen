package vn.edu.phenikaa.unicateen.domain.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.Clock;
import vn.edu.phenikaa.unicateen.domain.enums.Role;
import vn.edu.phenikaa.unicateen.domain.enums.UserStatus;

/** Tai khoan nguoi dung (UC5.1-UC5.3). Chi luu du lieu ca nhan can thiet (NFR12). */
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 100)
    private String email;
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;
    @Column(length = 10)
    private String phone;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;
    @Column(name = "card_code", length = 20)
    private String cardCode;
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;
    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected User() {
    }

    public static User create(String email, String fullName, String phone, Role role, String passwordHash) {
        User u = new User();
        u.email = email.trim().toLowerCase();
        u.fullName = fullName.trim();
        u.phone = phone;
        u.role = role;
        u.passwordHash = passwordHash;
        u.mustChangePassword = true;
        u.status = UserStatus.ACTIVE;
        u.createdAt = Clock.now();
        return u;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    /** BR43: dang trong thoi gian tam khoa do dang nhap sai. */
    public boolean isTemporarilyLocked(LocalDateTime now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    /** BR43: tang so lan sai; dat moc khoa tam khi dat nguong. */
    public void recordFailedLogin(int maxAttempts, int lockMinutes, LocalDateTime now) {
        failedAttempts++;
        if (failedAttempts >= maxAttempts) {
            lockedUntil = now.plusMinutes(lockMinutes);
            failedAttempts = 0;
        }
    }

    public void recordSuccessfulLogin(LocalDateTime now) {
        failedAttempts = 0;
        lockedUntil = null;
        lastLoginAt = now;
    }

    public void changePassword(String newHash) {
        passwordHash = newHash;
        mustChangePassword = false;
    }

    public void resetPassword(String tempHash) {
        passwordHash = tempHash;
        mustChangePassword = true;
    }

    public void lock() {
        status = UserStatus.LOCKED;
    }

    public void unlock() {
        status = UserStatus.ACTIVE;
        failedAttempts = 0;
        lockedUntil = null;
    }

    public void changeRole(Role newRole) {
        if (cardCode != null && !newRole.isCardHolder()) {
            throw new BusinessRuleException("BR45", "Tài khoản đang liên kết mã thẻ chỉ được là sinh viên hoặc giảng viên");
        }
        role = newRole;
    }

    /** BR45: ma the chi cho STUDENT, FACULTY. */
    public void linkCard(String code) {
        if (code != null && !role.isCardHolder()) {
            throw new BusinessRuleException("BR45", "Chỉ tài khoản sinh viên, giảng viên được liên kết mã thẻ");
        }
        cardCode = (code == null || code.isBlank()) ? null : code.trim();
    }

    public void updateProfile(String fullName, String phone) {
        this.fullName = fullName.trim();
        this.phone = phone;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public Role getRole() { return role; }
    public String getCardCode() { return cardCode; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public int getFailedAttempts() { return failedAttempts; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public UserStatus getStatus() { return status; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
