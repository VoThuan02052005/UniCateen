package vn.edu.phenikaa.unicateen.infrastructure.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import vn.edu.phenikaa.unicateen.application.port.IAuditLogger;
import vn.edu.phenikaa.unicateen.domain.model.AuditLog;
import vn.edu.phenikaa.unicateen.domain.model.User;
import vn.edu.phenikaa.unicateen.infrastructure.persistence.JpaAuditLogRepository;

/** Ghi nhat ky vao bang audit_logs trong cung giao dich voi thao tac nghiep vu (NFR09). */
@Component
public class DbAuditLogger implements IAuditLogger {
    private final JpaAuditLogRepository repo;
    private final ObjectMapper json;

    public DbAuditLogger(JpaAuditLogRepository repo, ObjectMapper json) {
        this.repo = repo;
        this.json = json;
    }

    @Override
    public void log(User actor, String action, String entityType, Long entityId, Object before, Object after) {
        repo.save(new AuditLog(actor, action, entityType, entityId, toJson(before), toJson(after)));
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Không ghi được nhật ký", e);
        }
    }
}
