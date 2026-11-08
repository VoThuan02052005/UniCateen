package vn.edu.phenikaa.unicateen.application.port;

import java.util.List;
import java.util.Optional;
import vn.edu.phenikaa.unicateen.domain.model.Supplier;

public interface SupplierRepository {
    Optional<Supplier> findById(Long id);

    List<Supplier> findAllByOrderByNameAsc();

    boolean existsByTaxCode(String taxCode);

    boolean existsByTaxCodeAndIdNot(String taxCode, Long id);

    long count();

    <S extends Supplier> S save(S supplier);

    void delete(Supplier supplier);
}
