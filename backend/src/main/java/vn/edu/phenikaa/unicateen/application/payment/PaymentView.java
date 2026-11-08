package vn.edu.phenikaa.unicateen.application.payment;

import java.time.LocalDateTime;
import vn.edu.phenikaa.unicateen.domain.enums.OrderStatus;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentMethod;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentStatus;
import vn.edu.phenikaa.unicateen.domain.model.Payment;

/** Giao dich thanh toan tra ve giao dien; {@code needsAttention} cho chuyen khoan cho qua 15 phut (BR27). */
public record PaymentView(Long id, String code, String orderCode, OrderStatus orderStatus, PaymentMethod method,
                          long amount, PaymentStatus status, String referenceCode, Long receivedAmount,
                          Long changeAmount, String failureReason, String payerName, String cardCode,
                          String confirmedByName, LocalDateTime transactionTime, LocalDateTime createdAt,
                          boolean needsAttention) {

    public static PaymentView of(Payment p, boolean needsAttention) {
        return new PaymentView(p.getId(), p.getCode(), p.getOrder().getCode(), p.getOrder().getStatus(),
                p.getMethod(), p.getAmount(), p.getStatus(), p.getReferenceCode(), p.getReceivedAmount(),
                p.getChangeAmount(), p.getFailureReason(), p.getPayer() == null ? null : p.getPayer().getFullName(),
                p.getCardCode(), p.getConfirmedBy() == null ? null : p.getConfirmedBy().getFullName(),
                p.getTransactionTime(), p.getCreatedAt(), needsAttention);
    }
}
