package vn.edu.phenikaa.unicateen.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.phenikaa.unicateen.application.port.PreparationRepository;
import vn.edu.phenikaa.unicateen.domain.model.Preparation;

public interface JpaPreparationRepository extends JpaRepository<Preparation, Long>, PreparationRepository {
}
