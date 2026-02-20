package app.partsvibe.testsupport.web;

import app.partsvibe.shared.security.CurrentUserProvider;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

public class InMemoryCurrentUserProvider implements CurrentUserProvider {
    private final ThreadLocal<String> currentUsername = new ThreadLocal<>();
    private final ThreadLocal<Set<String>> currentRoles = new ThreadLocal<>();

    @Override
    public Optional<String> currentUsername() {
        return Optional.ofNullable(currentUsername.get());
    }

    @Override
    public boolean isAuthenticated() {
        return currentUsername.get() != null;
    }

    @Override
    public boolean hasRole(String roleName) {
        return Optional.ofNullable(currentRoles.get())
                .orElseGet(Collections::emptySet)
                .contains(roleName);
    }

    public void setCurrentUser(String username, Set<String> roles) {
        currentUsername.set(username);
        currentRoles.set(Set.copyOf(roles));
    }

    public void setCurrentUser(String username) {
        setCurrentUser(username, Set.of());
    }

    public void clear() {
        currentUsername.remove();
        currentRoles.remove();
    }
}
