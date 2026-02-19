package app.partsvibe.users.errors;

import app.partsvibe.shared.error.ApplicationException;

public class UsernameAlreadyExistsException extends ApplicationException {
    public UsernameAlreadyExistsException(String username) {
        super("Username already exists: " + username);
    }
}
