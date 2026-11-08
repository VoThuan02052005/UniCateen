package vn.edu.phenikaa.unicateen.application.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import vn.edu.phenikaa.unicateen.domain.model.Order;

public interface OrderRepository {
    Optional<Order> findByCode(String code);

    /** SELECT ... FOR UPDATE: khoa ghi tren don khi doi trang thai. */
    Optional<Order> findByCodeForUpdate(String code);

    List<Order> findByCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            LocalDateTime from, LocalDateTime to);

    List<Order> findByCashierIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanOrderByCreatedAtDesc(
            Long cashierId, LocalDateTime from, LocalDateTime to);

    /** UC2.5: don gan voi tai khoan nguoi mua (BR16, BR17). */
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    /** Ma lon nhat co tien to, dung de sinh ma DH-yymmdd-nnnn. */
    String findMaxCodeWithPrefix(String prefix);

    <S extends Order> S save(S order);
}
