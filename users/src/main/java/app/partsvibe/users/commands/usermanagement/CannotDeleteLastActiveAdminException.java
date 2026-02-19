package app.partsvibe.users.commands.usermanagement;

import app.partsvibe.shared.error.ApplicationException;

public class CannotDeleteLastActiveAdminException extends ApplicationException {
    public CannotDeleteLastActiveAdminException() {
        super("Cannot delete the last active admin user.");
    }
}
