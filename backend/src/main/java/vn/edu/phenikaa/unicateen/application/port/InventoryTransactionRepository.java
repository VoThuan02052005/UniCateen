package vn.edu.phenikaa.unicateen.application.port;

import java.time.LocalDateTime;
import java.util.List;
import vn.edu.phenikaa.unicateen.domain.model.InventoryTransaction;

public interface InventoryTransactionRepository {
    /** BR07: nguyen lieu da co giao dich kho. */
    boolean existsByLotIngredientId(Long ingredientId);

    /** Ton dau ky theo nguyen lieu: tong quantity (co dau) truoc moc thoi gian. */
    List<IngredientQty> sumByIngredientBefore(LocalDateTime t);

    List<InventoryTransaction> findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(LocalDateTime from,
                                                                                    LocalDateTime to);

    <S extends InventoryTransaction> S save(S tx);
}
