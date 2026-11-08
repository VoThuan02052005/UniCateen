package vn.edu.phenikaa.unicateen.api.common;

import jakarta.validation.ConstraintViolationException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import vn.edu.phenikaa.unicateen.application.account.AuthService.LoginFailedException;
import vn.edu.phenikaa.unicateen.application.common.ExternalServiceException;
import vn.edu.phenikaa.unicateen.application.common.ForbiddenException;
import vn.edu.phenikaa.unicateen.application.common.NotFoundException;
import vn.edu.phenikaa.unicateen.domain.BusinessRuleException;

/** Chuyen ngoai le thanh ma HTTP va thong bao tieng Viet (Bang cac loai loi, Chuong 3). */
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> invalid(MethodArgumentNotValidException e) {
        Map<String, String> fields = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors().forEach(f -> fields.putIfAbsent(f.getField(), f.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError("INVALID", "Dữ liệu không hợp lệ", fields));
    }

    @ExceptionHandler({ConstraintViolationException.class, HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    public ResponseEntity<ApiError> badRequest(Exception e) {
        return ResponseEntity.badRequest().body(new ApiError("INVALID", "Dữ liệu không hợp lệ: " + e.getMessage()));
    }

    @ExceptionHandler(LoginFailedException.class)
    public ResponseEntity<ApiError> login(LoginFailedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError("LOGIN_FAILED", e.getMessage()));
    }

    @ExceptionHandler({ForbiddenException.class, AccessDeniedException.class})
    public ResponseEntity<ApiError> forbidden(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError("FORBIDDEN", e.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> notFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError("NOT_FOUND", e.getMessage()));
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> rule(BusinessRuleException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiError(e.getRule(), e.getMessage()));
    }

    /** Rang buoc CSDL la lop bao ve cuoi (UNIQUE active_order_id, idempotency_key, CHECK ...). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integrity(DataIntegrityViolationException e) {
        String msg = String.valueOf(e.getMostSpecificCause().getMessage());
        String code = msg.contains("uq_pay_active") ? "BR20"
                : msg.contains("uq_pay_idem") ? "NFR06"
                : msg.contains("uq_recipe_active") ? "BR09"
                : msg.contains("ck_lot_qty") ? "BR35"
                : "CONSTRAINT";
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError(code, "Dữ liệu vi phạm ràng buộc, vui lòng tải lại và thử lại"));
    }

    @ExceptionHandler(PessimisticLockingFailureException.class)
    public ResponseEntity<ApiError> locked(PessimisticLockingFailureException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("BUSY", "Dữ liệu đang được người khác xử lý, vui lòng thử lại"));
    }

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<ApiError> external(ExternalServiceException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(new ApiError("EXTERNAL", e.getMessage()));
    }

    /** Loi chuan cua Spring MVC (khong co duong dan, sai phuong thuc, thieu header Idempotency-Key ...). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(Exception e) {
        if (e instanceof ErrorResponse er) {
            return ResponseEntity.status(er.getStatusCode())
                    .body(new ApiError("HTTP_" + er.getStatusCode().value(), e.getMessage()));
        }
        log.error("Loi khong mong doi", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("ERROR", "Lỗi hệ thống, vui lòng thử lại"));
    }
}
