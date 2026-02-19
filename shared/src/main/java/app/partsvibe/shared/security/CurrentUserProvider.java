package app.partsvibe.shared.security;

import java.util.Optional;

public interface CurrentUserProvider {
    Optional<String> currentUsername();

    boolean isAuthenticated();

    boolean hasRole(String roleName);
}
