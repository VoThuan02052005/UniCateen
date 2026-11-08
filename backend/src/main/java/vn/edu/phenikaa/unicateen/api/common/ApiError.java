package vn.edu.phenikaa.unicateen.api.common;

import java.util.Map;

/** Than phan hoi loi: {@code code} la ma quy tac (vi du BR20) hoac loai loi; {@code fields} cho loi nhap lieu. */
public record ApiError(String code, String message, Map<String, String> fields) {
    public ApiError(String code, String message) {
        this(code, message, null);
    }
}
