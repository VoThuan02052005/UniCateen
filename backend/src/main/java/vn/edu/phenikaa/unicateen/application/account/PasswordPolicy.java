package vn.edu.phenikaa.unicateen.application.account;

import java.security.SecureRandom;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;

/** Quy tac mat khau ung dung (NFR03): toi thieu 8 ky tu, co chu va so. */
public final class PasswordPolicy {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private PasswordPolicy() {
    }

    public static void check(String pw) {
        if (pw == null || pw.length() < 8 || !pw.matches(".*[A-Za-z].*") || !pw.matches(".*\\d.*")) {
            throw new BusinessRuleException("NFR03", "Mật khẩu tối thiểu 8 ký tự, gồm cả chữ và số");
        }
    }

    /** Mat khau tam cho tai khoan moi hoac khi dat lai; nguoi dung phai doi o lan dang nhap dau. */
    public static String temporary() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return sb.append(RANDOM.nextInt(10)).toString();
    }
}
