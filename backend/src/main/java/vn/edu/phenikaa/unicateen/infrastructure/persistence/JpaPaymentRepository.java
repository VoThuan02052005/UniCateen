package vn.edu.phenikaa.unicateen.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.phenikaa.unicateen.application.port.PaymentRepository;
import vn.edu.phenikaa.unicateen.domain.model.Payment;

public interface JpaPaymentRepository extends JpaRepository<Payment, Long>, PaymentRepository {
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.code = :code")
    Optional<Payment> findByCodeForUpdate(@Param("code") String code);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Payment p
            where p.status = vn.edu.phenikaa.unicateen.domain.enums.PaymentStatus.PENDING
              and p.method = vn.edu.phenikaa.unicateen.domain.enums.PaymentMethod.STUDENT_FACULTY_CARD
              and p.createdAt < :limit""")
    List<Payment> findPendingCardCreatedBeforeForUpdate(@Param("limit") LocalDateTime limit);

    @Override
    @Query("select max(e.code) from Payment e where e.code like concat(:prefix, '%')")
    String findMaxCodeWithPrefix(@Param("prefix") String prefix);

    @Override
    @Query("select p.order.code from Payment p where p.code = :code")
    String findOrderCodeByPaymentCode(@Param("code") String paymentCode);
}
