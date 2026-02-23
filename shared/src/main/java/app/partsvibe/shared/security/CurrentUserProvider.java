package app.partsvibe.shared.security;

import java.util.Optional;

public interface CurrentUserProvider {
    Optional<Long> currentUserId();

    Optional<String> currentUsername();

    boolean isAuthenticated();

    boolean hasRole(String roleName);
}
