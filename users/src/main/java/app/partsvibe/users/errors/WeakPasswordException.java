package app.partsvibe.users.errors;

import app.partsvibe.shared.error.ApplicationException;

public class WeakPasswordException extends ApplicationException {
    public WeakPasswordException(String message) {
        super(message);
    }
}
