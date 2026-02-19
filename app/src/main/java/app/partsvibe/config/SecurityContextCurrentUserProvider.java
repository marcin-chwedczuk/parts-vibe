package app.partsvibe.config;

import app.partsvibe.shared.security.CurrentUserProvider;
import java.util.Optional;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityContextCurrentUserProvider implements CurrentUserProvider {
    @Override
    public boolean isAuthenticated() {
        return currentAuthentication().isPresent();
    }

    @Override
    public Optional<String> currentUsername() {
        return currentAuthentication().map(Authentication::getName).filter(name -> !name.isBlank());
    }

    @Override
    public boolean hasRole(String roleName) {
        return currentAuthentication().stream()
                .flatMap(authentication -> authentication.getAuthorities().stream())
                .anyMatch(authority -> roleName.equals(authority.getAuthority()));
    }

    private Optional<Authentication> currentAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        return Optional.of(authentication);
    }
}
