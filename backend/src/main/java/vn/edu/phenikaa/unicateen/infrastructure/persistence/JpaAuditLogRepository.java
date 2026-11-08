package vn.edu.phenikaa.unicateen.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.phenikaa.unicateen.application.port.AuditLogRepository;
import vn.edu.phenikaa.unicateen.domain.model.AuditLog;

public interface JpaAuditLogRepository extends JpaRepository<AuditLog, Long>, AuditLogRepository {
}
