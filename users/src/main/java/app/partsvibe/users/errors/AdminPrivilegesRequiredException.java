package app.partsvibe.users.errors;

import app.partsvibe.shared.error.ApplicationException;

public class AdminPrivilegesRequiredException extends ApplicationException {
    public AdminPrivilegesRequiredException(Long userId) {
        super("Admin privileges are required. userId=" + userId);
    }
}
