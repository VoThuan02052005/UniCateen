package vn.edu.phenikaa.unicateen.application.port;

import java.util.List;
import vn.edu.phenikaa.unicateen.domain.model.StockAdjustment;

public interface StockAdjustmentRepository {
    List<StockAdjustment> findTop200ByOrderByIdDesc();

    String findMaxCodeWithPrefix(String prefix);

    <S extends StockAdjustment> S saveAndFlush(S adjustment);
}
