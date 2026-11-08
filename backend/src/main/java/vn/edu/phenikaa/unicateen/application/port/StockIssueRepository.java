package vn.edu.phenikaa.unicateen.application.port;

import java.util.List;
import java.util.Optional;
import vn.edu.phenikaa.unicateen.domain.model.StockIssue;

public interface StockIssueRepository {
    Optional<StockIssue> findById(Long id);

    Optional<StockIssue> findByIdForUpdate(Long id);

    List<StockIssue> findTop200ByOrderByIdDesc();

    String findMaxCodeWithPrefix(String prefix);

    <S extends StockIssue> S save(S issue);
}
