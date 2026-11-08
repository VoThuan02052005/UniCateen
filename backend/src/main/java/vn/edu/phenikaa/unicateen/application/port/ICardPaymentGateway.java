package vn.edu.phenikaa.unicateen.application.port;

/**
 * He thong con the (GD2). Ban hien tai la SimulatedCardGateway; khi truong cap quyen se thay bang tich hop that.
 * Cai dat phai tu ap thoi gian cho toi da 10 giay va nem CardGatewayException khi qua han hoac loi ket noi.
 */
public interface ICardPaymentGateway {
    CardResult charge(String cardCode, long amount, String orderCode, String idempotencyKey);

    record CardResult(boolean approved, String reference, String message) {
    }

    class CardGatewayException extends RuntimeException {
        public CardGatewayException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
