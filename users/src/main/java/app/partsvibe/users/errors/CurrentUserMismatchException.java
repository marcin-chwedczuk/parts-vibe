package app.partsvibe.users.errors;

import app.partsvibe.shared.error.ApplicationException;

public class CurrentUserMismatchException extends ApplicationException {
    public CurrentUserMismatchException(Long commandUserId, Long currentUserId) {
        super("Current authenticated user does not match command user. commandUserId=%s, currentUserId=%s"
                .formatted(commandUserId, currentUserId));
    }
}
