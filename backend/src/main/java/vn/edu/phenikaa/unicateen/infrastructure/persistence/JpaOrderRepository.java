package vn.edu.phenikaa.unicateen.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.phenikaa.unicateen.application.port.OrderRepository;
import vn.edu.phenikaa.unicateen.domain.model.Order;

public interface JpaOrderRepository extends JpaRepository<Order, Long>, OrderRepository {
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.code = :code")
    Optional<Order> findByCodeForUpdate(@Param("code") String code);

    @Override
    @Query("select max(e.code) from Order e where e.code like concat(:prefix, '%')")
    String findMaxCodeWithPrefix(@Param("prefix") String prefix);
}
