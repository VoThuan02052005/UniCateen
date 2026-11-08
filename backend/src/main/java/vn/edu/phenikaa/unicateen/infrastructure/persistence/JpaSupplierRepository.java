package vn.edu.phenikaa.unicateen.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.phenikaa.unicateen.application.port.SupplierRepository;
import vn.edu.phenikaa.unicateen.domain.model.Supplier;

public interface JpaSupplierRepository extends JpaRepository<Supplier, Long>, SupplierRepository {
}
