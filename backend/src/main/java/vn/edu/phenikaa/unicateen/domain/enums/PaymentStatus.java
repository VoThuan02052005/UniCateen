package vn.edu.phenikaa.unicateen.domain.enums;

/** Trang thai giao dich thanh toan. */
public enum PaymentStatus {
    PENDING, SUCCESS, FAILED, REFUNDED;

    /** BR20: giao dich dang hoat dong. REFUNDED khong dung o phien ban nay (GD4). */
    public boolean isActive() {
        return this == PENDING || this == SUCCESS;
    }
}
