package vn.edu.phenikaa.unicateen.infrastructure.persistence;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.phenikaa.unicateen.application.port.IngredientQty;
import vn.edu.phenikaa.unicateen.application.port.InventoryTransactionRepository;
import vn.edu.phenikaa.unicateen.domain.model.InventoryTransaction;

public interface JpaInventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long>, InventoryTransactionRepository {
    @Override
    @Query("""
            select new vn.edu.phenikaa.unicateen.application.port.IngredientQty(t.lot.ingredient.id, sum(t.quantity))
            from InventoryTransaction t where t.createdAt < :t group by t.lot.ingredient.id""")
    List<IngredientQty> sumByIngredientBefore(@Param("t") LocalDateTime t);
}
