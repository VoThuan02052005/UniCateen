package vn.edu.phenikaa.unicateen.application.port;

import java.util.List;
import vn.edu.phenikaa.unicateen.domain.model.AuditLog;

/** Chi doc nhat ky; ghi qua IAuditLogger. Khong co phuong thuc sua, xoa (NFR09). */
public interface AuditLogRepository {
    List<AuditLog> findTop300ByOrderByIdDesc();
}
