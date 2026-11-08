package vn.edu.phenikaa.unicateen.infrastructure.persistence;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.phenikaa.unicateen.application.port.StockLotRepository;
import vn.edu.phenikaa.unicateen.domain.model.StockLot;

public interface JpaStockLotRepository extends JpaRepository<StockLot, Long>, StockLotRepository {
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from StockLot l where l.id = :id")
    Optional<StockLot> findByIdForUpdate(@Param("id") Long id);

    /** BR34: FEFO, lo khong co han dung xep sau cung. */
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select l from StockLot l
            where l.ingredient.id = :ingredientId and l.remainingQty > 0
              and (l.expiryDate is null or l.expiryDate >= :today)
            order by case when l.expiryDate is null then 1 else 0 end, l.expiryDate, l.receivedDate, l.id""")
    List<StockLot> findUsableFefoForUpdate(@Param("ingredientId") Long ingredientId, @Param("today") LocalDate today);

    @Override
    @Query("select l from StockLot l join fetch l.ingredient where l.remainingQty > 0 order by l.ingredient.name, l.expiryDate")
    List<StockLot> findInStock();

    @Override
    @Query("select max(e.code) from StockLot e where e.code like concat(:prefix, '%')")
    String findMaxCodeWithPrefix(@Param("prefix") String prefix);
}
