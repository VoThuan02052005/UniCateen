package vn.edu.phenikaa.unicateen.application.port;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import vn.edu.phenikaa.unicateen.domain.model.StockLot;

public interface StockLotRepository {
    Optional<StockLot> findById(Long id);

    Optional<StockLot> findByIdForUpdate(Long id);

    /** BR34: lo con hang, chua qua han, theo thu tu FEFO; khoa ghi. */
    List<StockLot> findUsableFefoForUpdate(Long ingredientId, LocalDate today);

    /** Lo con hang (ke ca qua han) de theo doi ton. */
    List<StockLot> findInStock();

    String findMaxCodeWithPrefix(String prefix);

    <S extends StockLot> S save(S lot);
}
