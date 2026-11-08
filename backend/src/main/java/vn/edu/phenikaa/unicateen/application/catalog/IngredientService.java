package vn.edu.phenikaa.unicateen.application.catalog;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.phenikaa.unicateen.application.catalog.CatalogViews.IngredientView;
import vn.edu.phenikaa.unicateen.application.common.CodeGenerator;
import vn.edu.phenikaa.unicateen.application.common.NotFoundException;
import vn.edu.phenikaa.unicateen.application.port.IngredientRepository;
import vn.edu.phenikaa.unicateen.application.port.InventoryTransactionRepository;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;
import vn.edu.phenikaa.unicateen.domain.enums.ActiveStatus;
import vn.edu.phenikaa.unicateen.domain.model.Ingredient;

/** UC1.4 Quan ly nguyen lieu. */
@Service
@Transactional
public class IngredientService {
    private final IngredientRepository ingredients;
    private final InventoryTransactionRepository transactions;

    public IngredientService(IngredientRepository ingredients, InventoryTransactionRepository transactions) {
        this.ingredients = ingredients;
        this.transactions = transactions;
    }

    @Transactional(readOnly = true)
    public List<IngredientView> list() {
        return ingredients.findAllByOrderByGroupNameAscNameAsc().stream().map(IngredientView::of).toList();
    }

    public IngredientView create(String name, String groupName, String unit, BigDecimal minStock, boolean trackExpiry,
                                 String storageNote) {
        if (ingredients.existsByNameIgnoreCase(name.trim())) {
            throw new BusinessRuleException("DV06", "Tên nguyên liệu đã tồn tại");
        }
        List<String> codes = ingredients.findAllByOrderByGroupNameAscNameAsc().stream()
                .map(Ingredient::getCode).toList();
        Ingredient i = new Ingredient(CodeGenerator.sequential("NL", 3, codes), name, groupName, unit, minStock,
                trackExpiry, storageNote);
        return IngredientView.of(ingredients.save(i));
    }

    public IngredientView update(Long id, String name, String groupName, String unit, BigDecimal minStock,
                                 boolean trackExpiry, String storageNote) {
        Ingredient i = get(id);
        if (ingredients.existsByNameIgnoreCaseAndIdNot(name.trim(), id)) {
            throw new BusinessRuleException("DV06", "Tên nguyên liệu đã tồn tại");
        }
        i.changeUnit(unit, transactions.existsByLotIngredientId(id));          // BR07
        i.update(name, groupName, minStock, trackExpiry, storageNote);
        return IngredientView.of(i);
    }

    public IngredientView setStatus(Long id, ActiveStatus status) {
        Ingredient i = get(id);
        i.setStatus(status);
        return IngredientView.of(i);
    }

    public Ingredient get(Long id) {
        return ingredients.findById(id).orElseThrow(() -> new NotFoundException("nguyên liệu", id));
    }
}
