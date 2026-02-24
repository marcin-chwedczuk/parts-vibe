package app.partsvibe.users.errors;

import app.partsvibe.shared.error.ApplicationException;

public class InvalidCurrentPasswordException extends ApplicationException {
    public InvalidCurrentPasswordException() {
        super("Current password is invalid.");
    }
}
