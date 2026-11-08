package vn.edu.phenikaa.unicateen.api.account;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.phenikaa.unicateen.application.account.AccountService;
import vn.edu.phenikaa.unicateen.application.account.AccountService.*;
import vn.edu.phenikaa.unicateen.application.common.CurrentUser;
import vn.edu.phenikaa.unicateen.domain.enums.Role;

/** UC5.2 Quan ly tai khoan, UC5.3 Phan quyen (ADMIN). */
@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    public record CreateRequest(@NotBlank @Email @Size(max = 100) String email,
                                @NotBlank @Size(min = 2, max = 100) String fullName,
                                @Pattern(regexp = "^$|0\\d{9}", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0")
                                String phone,
                                @NotNull Role role, @Size(max = 20) String cardCode) {
    }

    public record UpdateRequest(@NotBlank @Size(min = 2, max = 100) String fullName,
                                @Pattern(regexp = "^$|0\\d{9}", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0")
                                String phone,
                                @Size(max = 20) String cardCode) {
    }

    public record RoleRequest(@NotNull Role role) {
    }

    @GetMapping
    public List<AccountView> list() {
        return accounts.list();
    }

    @PostMapping
    public CreatedAccount create(@Valid @RequestBody CreateRequest r) {
        return accounts.create(r.email(), r.fullName(), r.phone(), r.role(), r.cardCode());
    }

    @PutMapping("/{id}")
    public AccountView update(@PathVariable Long id, @Valid @RequestBody UpdateRequest r) {
        return accounts.updateProfile(id, r.fullName(), r.phone(), r.cardCode());
    }

    @PutMapping("/{id}/role")
    public AccountView changeRole(@PathVariable Long id, @Valid @RequestBody RoleRequest r,
                                  @AuthenticationPrincipal CurrentUser me) {
        return accounts.changeRole(id, r.role(), me);
    }

    @PostMapping("/{id}/lock")
    public AccountView lock(@PathVariable Long id, @AuthenticationPrincipal CurrentUser me) {
        return accounts.lock(id, me);
    }

    @PostMapping("/{id}/unlock")
    public AccountView unlock(@PathVariable Long id, @AuthenticationPrincipal CurrentUser me) {
        return accounts.unlock(id, me);
    }

    @PostMapping("/{id}/reset-password")
    public Map<String, String> resetPassword(@PathVariable Long id) {
        return Map.of("temporaryPassword", accounts.resetPassword(id));
    }

    /** Nhap danh sach sinh vien, giang vien tu tep CSV (cot: email, full_name, role, card_code). */
    @PostMapping(value = "/import", consumes = "multipart/form-data")
    public ImportResult importCsv(@RequestParam("file") MultipartFile file) throws IOException {
        return accounts.importCsv(new String(file.getBytes(), StandardCharsets.UTF_8));
    }

    @GetMapping("/audit-logs")
    public List<AuditLogView> auditLogs() {
        return accounts.auditLogs();
    }
}
