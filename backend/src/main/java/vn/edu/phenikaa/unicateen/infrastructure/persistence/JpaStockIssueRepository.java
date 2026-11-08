package vn.edu.phenikaa.unicateen.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.phenikaa.unicateen.application.port.StockIssueRepository;
import vn.edu.phenikaa.unicateen.domain.model.StockIssue;

public interface JpaStockIssueRepository extends JpaRepository<StockIssue, Long>, StockIssueRepository {
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from StockIssue i where i.id = :id")
    Optional<StockIssue> findByIdForUpdate(@Param("id") Long id);

    @Override
    @Query("select max(e.code) from StockIssue e where e.code like concat(:prefix, '%')")
    String findMaxCodeWithPrefix(@Param("prefix") String prefix);
}
