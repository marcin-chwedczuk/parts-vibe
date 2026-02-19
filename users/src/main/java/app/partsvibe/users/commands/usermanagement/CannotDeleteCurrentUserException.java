package app.partsvibe.users.commands.usermanagement;

import app.partsvibe.shared.error.ApplicationException;

public class CannotDeleteCurrentUserException extends ApplicationException {
    public CannotDeleteCurrentUserException() {
        super("Current user cannot delete own account.");
    }
}
