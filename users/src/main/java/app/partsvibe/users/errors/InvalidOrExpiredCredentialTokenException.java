package app.partsvibe.users.errors;

import app.partsvibe.shared.error.ApplicationException;

public class InvalidOrExpiredCredentialTokenException extends ApplicationException {
    public InvalidOrExpiredCredentialTokenException() {
        super("Credential token is invalid or expired.");
    }
}
