package vn.edu.phenikaa.unicateen.application.port;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import vn.edu.phenikaa.unicateen.domain.enums.PreparationStatus;
import vn.edu.phenikaa.unicateen.domain.model.Preparation;

public interface PreparationRepository {
    Optional<Preparation> findById(Long id);

    Optional<Preparation> findByOrderId(Long orderId);

    List<Preparation> findByStatusInOrderByOrderPaidAtAsc(Collection<PreparationStatus> statuses);

    <S extends Preparation> S save(S preparation);
}
