package vn.edu.phenikaa.unicateen.application.common;

/** Khong du quyen tren du lieu cu the (lop kiem tra so huu, BR17); tang api tra 403. */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
