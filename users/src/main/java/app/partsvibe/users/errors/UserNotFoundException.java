package app.partsvibe.users.errors;

import app.partsvibe.shared.error.ApplicationException;

public class UserNotFoundException extends ApplicationException {
    public UserNotFoundException(Long userId) {
        super("User not found: " + userId);
    }
}
