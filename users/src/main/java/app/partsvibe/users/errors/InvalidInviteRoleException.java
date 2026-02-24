package app.partsvibe.users.errors;

import app.partsvibe.shared.error.ApplicationException;

public class InvalidInviteRoleException extends ApplicationException {
    public InvalidInviteRoleException(String roleName) {
        super("Invite role is invalid: " + roleName);
    }
}
