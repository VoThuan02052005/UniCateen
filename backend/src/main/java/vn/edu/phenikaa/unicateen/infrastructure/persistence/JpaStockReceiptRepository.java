package vn.edu.phenikaa.unicateen.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.phenikaa.unicateen.application.port.StockReceiptRepository;
import vn.edu.phenikaa.unicateen.domain.model.StockReceipt;

public interface JpaStockReceiptRepository extends JpaRepository<StockReceipt, Long>, StockReceiptRepository {
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from StockReceipt r where r.id = :id")
    Optional<StockReceipt> findByIdForUpdate(@Param("id") Long id);

    @Override
    @Query("select max(e.code) from StockReceipt e where e.code like concat(:prefix, '%')")
    String findMaxCodeWithPrefix(@Param("prefix") String prefix);
}
