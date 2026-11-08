package vn.edu.phenikaa.unicateen.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.phenikaa.unicateen.application.port.StockAdjustmentRepository;
import vn.edu.phenikaa.unicateen.domain.model.StockAdjustment;

public interface JpaStockAdjustmentRepository extends JpaRepository<StockAdjustment, Long>, StockAdjustmentRepository {
    @Override
    @Query("select max(e.code) from StockAdjustment e where e.code like concat(:prefix, '%')")
    String findMaxCodeWithPrefix(@Param("prefix") String prefix);
}
