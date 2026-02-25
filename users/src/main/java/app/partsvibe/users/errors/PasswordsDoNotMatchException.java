package app.partsvibe.users.errors;

import app.partsvibe.shared.error.ApplicationException;

public class PasswordsDoNotMatchException extends ApplicationException {
    public PasswordsDoNotMatchException() {
        super("Passwords do not match.");
    }
}
