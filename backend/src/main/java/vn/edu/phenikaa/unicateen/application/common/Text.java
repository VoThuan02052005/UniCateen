package vn.edu.phenikaa.unicateen.application.common;

/** Tien ich chuan hoa chuoi nhap tu giao dien. */
public final class Text {
    private Text() {
    }

    public static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
