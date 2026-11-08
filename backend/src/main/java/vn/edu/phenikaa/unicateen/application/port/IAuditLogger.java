package vn.edu.phenikaa.unicateen.application.port;

import vn.edu.phenikaa.unicateen.domain.model.User;

/** Ghi nhat ky kiem toan (NFR09); before, after duoc ghi dang JSON. */
public interface IAuditLogger {
    void log(User actor, String action, String entityType, Long entityId, Object before, Object after);
}
