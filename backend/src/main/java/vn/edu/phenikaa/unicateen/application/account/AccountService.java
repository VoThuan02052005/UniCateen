package vn.edu.phenikaa.unicateen.application.account;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phenikaa.unicateen.application.common.Actors;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;
import vn.edu.phenikaa.unicateen.application.common.NotFoundException;
import vn.edu.phenikaa.unicateen.application.common.Text;
import vn.edu.phenikaa.unicateen.application.port.AuditLogRepository;
import vn.edu.phenikaa.unicateen.application.port.IAuditLogger;
import vn.edu.phenikaa.unicateen.application.port.IPasswordHasher;
import vn.edu.phenikaa.unicateen.application.port.UserRepository;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.enums.Role;
import vn.edu.phenikaa.unicateen.domain.enums.UserStatus;
import vn.edu.phenikaa.unicateen.domain.model.AuditLog;
import vn.edu.phenikaa.unicateen.domain.model.User;

/** UC5.2 Quan ly tai khoan, UC5.3 Phan quyen (BR40, BR42, BR44, BR45). */
@Service
@Transactional
public class AccountService {
    private static final String EMAIL = "^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$";      // DV01

    private final UserRepository users;
    private final AuditLogRepository auditLogs;
    private final IPasswordHasher hasher;
    private final IAuditLogger audit;
    private final Actors actors;

    public AccountService(UserRepository users, AuditLogRepository auditLogs, IPasswordHasher hasher,
                          IAuditLogger audit, Actors actors) {
        this.users = users;
        this.auditLogs = auditLogs;
        this.hasher = hasher;
        this.audit = audit;
        this.actors = actors;
    }

    public record AccountView(Long id, String email, String fullName, String phone, Role role, String cardCode,
                              UserStatus status, boolean mustChangePassword, LocalDateTime lastLoginAt) {
        static AccountView of(User u) {
            return new AccountView(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getRole(),
                    u.getCardCode(), u.getStatus(), u.isMustChangePassword(), u.getLastLoginAt());
        }
    }

    /** Tai khoan moi kem mat khau tam, chi tra ve mot lan. */
    public record CreatedAccount(AccountView account, String temporaryPassword) {
    }

    public record ImportResult(int created, int skipped, List<String> errors) {
    }

    public record AuditLogView(Long id, String actor, String action, String entityType, Long entityId,
                               String beforeValue, String afterValue, LocalDateTime createdAt) {
    }

    @Transactional(readOnly = true)
    public List<AccountView> list() {
        return users.findAllByOrderByIdAsc().stream().map(AccountView::of).toList();
    }

    @Transactional(readOnly = true)
    public AccountView me(CurrentUser actor) {
        return AccountView.of(actors.load(actor));
    }

    public CreatedAccount create(String email, String fullName, String phone, Role role, String cardCode) {
        String mail = email.trim().toLowerCase(Locale.ROOT);
        if (!mail.matches(EMAIL)) {
            throw new BusinessRuleException("DV01", "Email không đúng định dạng");
        }
        if (users.existsByEmail(mail)) {
            throw new BusinessRuleException("BR40", "Email đã được dùng cho tài khoản khác");
        }
        String temp = PasswordPolicy.temporary();
        User u = User.create(mail, fullName, Text.blankToNull(phone), role, hasher.hash(temp));
        linkCard(u, cardCode);
        return new CreatedAccount(AccountView.of(users.save(u)), temp);
    }

    public AccountView updateProfile(Long id, String fullName, String phone, String cardCode) {
        User u = get(id);
        u.updateProfile(fullName, Text.blankToNull(phone));
        linkCard(u, cardCode);
        return AccountView.of(u);
    }

    /** UC5.3: doi vai tro, ghi nhat ky; khong tu doi vai tro cua minh, luon con mot ADMIN (BR44). */
    public AccountView changeRole(Long id, Role role, CurrentUser actor) {
        User u = get(id);
        if (u.getId().equals(actor.id())) {
            throw new BusinessRuleException("BR44", "Quản trị viên không tự đổi vai trò của chính mình");
        }
        if (u.getRole() == Role.ADMIN && role != Role.ADMIN) {
            ensureAnotherActiveAdmin(u);
        }
        Role before = u.getRole();
        u.changeRole(role);
        audit.log(actors.load(actor), "ROLE_CHANGE", "users", id, Map.of("role", before.name()),
                Map.of("role", role.name()));
        return AccountView.of(u);
    }

    /** Khoa tai khoan (BR44: khong xoa); ghi nhat ky. */
    public AccountView lock(Long id, CurrentUser actor) {
        User u = get(id);
        if (u.getId().equals(actor.id())) {
            throw new BusinessRuleException("BR44", "Quản trị viên không tự khóa tài khoản của chính mình");
        }
        if (u.getRole() == Role.ADMIN) {
            ensureAnotherActiveAdmin(u);
        }
        u.lock();
        audit.log(actors.load(actor), "ACCOUNT_LOCK", "users", id, Map.of("status", "ACTIVE"),
                Map.of("status", "LOCKED"));
        return AccountView.of(u);
    }

    public AccountView unlock(Long id, CurrentUser actor) {
        User u = get(id);
        u.unlock();
        audit.log(actors.load(actor), "ACCOUNT_UNLOCK", "users", id, Map.of("status", "LOCKED"),
                Map.of("status", "ACTIVE"));
        return AccountView.of(u);
    }

    /** Dat lai mat khau tam; tra ve mat khau tam mot lan. */
    public String resetPassword(Long id) {
        User u = get(id);
        String temp = PasswordPolicy.temporary();
        u.resetPassword(hasher.hash(temp));
        u.unlock();
        return temp;
    }

    /**
     * Nhap danh sach sinh vien, giang vien tu CSV co dong tieu de. Chi doc cac cot email, full_name, role,
     * card_code; cac cot khac bi bo qua de khong luu du lieu ca nhan thua (NFR12).
     */
    public ImportResult importCsv(String csv) {
        List<String> errors = new ArrayList<>();
        int created = 0;
        int skipped = 0;
        try (BufferedReader r = new BufferedReader(new StringReader(csv.replace("﻿", "")))) {
            String header = r.readLine();
            if (header == null) {
                return new ImportResult(0, 0, List.of("Tệp rỗng"));
            }
            List<String> cols = Arrays.stream(header.split(",")).map(s -> s.trim().toLowerCase(Locale.ROOT)).toList();
            int iEmail = cols.indexOf("email");
            int iName = cols.indexOf("full_name");
            int iRole = cols.indexOf("role");
            int iCard = cols.indexOf("card_code");
            if (iEmail < 0 || iName < 0 || iRole < 0) {
                return new ImportResult(0, 0, List.of("Thiếu cột bắt buộc: email, full_name, role"));
            }
            String line;
            int row = 1;
            while ((line = r.readLine()) != null) {
                row++;
                if (line.isBlank()) {
                    continue;
                }
                String[] f = line.split(",", -1);
                try {
                    String email = f[iEmail].trim();
                    if (users.existsByEmail(email.toLowerCase(Locale.ROOT))) {
                        skipped++;
                        continue;
                    }
                    Role role = Role.valueOf(f[iRole].trim().toUpperCase(Locale.ROOT));
                    if (!role.isCardHolder()) {
                        throw new BusinessRuleException("BR45", "Tệp chỉ dùng cho sinh viên, giảng viên");
                    }
                    String card = iCard >= 0 && iCard < f.length ? f[iCard] : null;
                    create(email, f[iName], null, role, card);
                    created++;
                } catch (RuntimeException e) {
                    errors.add("Dòng " + row + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            errors.add(e.getMessage());
        }
        return new ImportResult(created, skipped, errors);
    }

    /** Nhat ky chi doc; khong co API sua, xoa (NFR09). */
    @Transactional(readOnly = true)
    public List<AuditLogView> auditLogs() {
        return auditLogs.findTop300ByOrderByIdDesc().stream().map((AuditLog a) -> new AuditLogView(a.getId(),
                a.getActor().getFullName(), a.getAction(), a.getEntityType(), a.getEntityId(), a.getBeforeValue(),
                a.getAfterValue(), a.getCreatedAt())).toList();
    }

    private void linkCard(User u, String cardCode) {
        String card = Text.blankToNull(cardCode);
        if (card != null && !card.equals(u.getCardCode()) && users.existsByCardCode(card)) {
            throw new BusinessRuleException("BR45", "Mã thẻ đã liên kết với tài khoản khác");
        }
        u.linkCard(card);
    }

    private void ensureAnotherActiveAdmin(User u) {
        long active = users.countByRoleAndStatus(Role.ADMIN, UserStatus.ACTIVE);
        if (u.isActive() && active <= 1) {
            throw new BusinessRuleException("BR44", "Hệ thống phải còn ít nhất một quản trị viên đang hoạt động");
        }
    }

    private User get(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("tài khoản", id));
    }
}
