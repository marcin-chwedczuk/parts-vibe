package app.partsvibe.shared.email;

import app.partsvibe.shared.error.ApplicationException;

public class EmailSenderException extends ApplicationException {
    public EmailSenderException(String message, Throwable cause) {
        super(message, cause);
    }
}
