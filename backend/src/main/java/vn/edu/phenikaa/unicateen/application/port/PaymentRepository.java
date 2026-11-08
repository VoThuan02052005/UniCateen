package vn.edu.phenikaa.unicateen.application.port;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentMethod;
import vn.edu.phenikaa.unicateen.domain.enums.PaymentStatus;
import vn.edu.phenikaa.unicateen.domain.model.Payment;

public interface PaymentRepository {
    Optional<Payment> findByIdempotencyKey(String key);

    Optional<Payment> findByCode(String code);

    Optional<Payment> findByCodeForUpdate(String code);

    /** BR20: don co giao dich PENDING hoac SUCCESS. */
    boolean existsByOrderIdAndStatusIn(Long orderId, Collection<PaymentStatus> statuses);

    List<Payment> findByOrderIdOrderByCreatedAtDesc(Long orderId);

    /** BR27: giao dich the PENDING tao truoc moc thoi gian, khoa ghi. */
    List<Payment> findPendingCardCreatedBeforeForUpdate(LocalDateTime limit);

    List<Payment> findByMethodAndStatusOrderByCreatedAtAsc(PaymentMethod method, PaymentStatus status);

    List<Payment> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to);

    /** BR29: giao dich SUCCESS theo thoi diem thanh cong. */
    List<Payment> findByStatusAndTransactionTimeGreaterThanEqualAndTransactionTimeLessThan(
            PaymentStatus status, LocalDateTime from, LocalDateTime to);

    String findMaxCodeWithPrefix(String prefix);

    String findOrderCodeByPaymentCode(String paymentCode);

    <S extends Payment> S save(S payment);

    <S extends Payment> S saveAndFlush(S payment);
}
