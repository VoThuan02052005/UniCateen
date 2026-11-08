package vn.edu.phenikaa.unicateen.application.port;

import java.util.List;
import java.util.Optional;
import vn.edu.phenikaa.unicateen.domain.model.StockReceipt;

public interface StockReceiptRepository {
    Optional<StockReceipt> findById(Long id);

    Optional<StockReceipt> findByIdForUpdate(Long id);

    List<StockReceipt> findTop200ByOrderByIdDesc();

    boolean existsBySupplierId(Long supplierId);

    String findMaxCodeWithPrefix(String prefix);

    <S extends StockReceipt> S save(S receipt);
}
