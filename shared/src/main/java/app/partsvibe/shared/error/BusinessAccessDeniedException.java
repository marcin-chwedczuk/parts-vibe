package app.partsvibe.shared.error;

public class BusinessAccessDeniedException extends ApplicationException {
    public BusinessAccessDeniedException(String message) {
        super(message);
    }

    public BusinessAccessDeniedException(String message, Throwable cause) {
        super(message, cause);
    }
}
