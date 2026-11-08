package vn.edu.phenikaa.unicateen.application.common;

import java.io.Serializable;
import vn.edu.phenikaa.unicateen.domain.enums.Role;

/** Nguoi dung dang dang nhap; luu trong phien (principal cua Spring Security). */
public record CurrentUser(Long id, String email, String fullName, Role role) implements Serializable {
    public boolean is(Role r) {
        return role == r;
    }
}
