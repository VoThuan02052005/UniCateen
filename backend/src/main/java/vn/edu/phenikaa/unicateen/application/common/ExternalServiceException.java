package vn.edu.phenikaa.unicateen.application.common;

/** Loi dich vu ngoai (dich vu the); tang api tra 502. */
public class ExternalServiceException extends RuntimeException {
    public ExternalServiceException(String message) {
        super(message);
    }
}
