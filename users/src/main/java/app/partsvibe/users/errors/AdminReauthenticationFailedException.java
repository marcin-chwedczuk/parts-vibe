package app.partsvibe.users.errors;

import app.partsvibe.shared.error.ApplicationException;

public class AdminReauthenticationFailedException extends ApplicationException {
    public AdminReauthenticationFailedException() {
        super("Admin re-authentication failed.");
    }
}
