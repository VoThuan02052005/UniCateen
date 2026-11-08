package vn.edu.phenikaa.unicateen.application.common;

/** Khong tim thay doi tuong; tang api tra 404. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String what, Object key) {
        super("Không tìm thấy " + what + " " + key);
    }
}
