package app.partsvibe.shared.security;

import java.util.function.BiFunction;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class AuthorizationService {
    private final CurrentUserProvider currentUserProvider;

    public AuthorizationService(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    public void assertCurrentUserHasId(
            Long expectedUserId, BiFunction<Long, Long, ? extends RuntimeException> exceptionFactory) {
        Long currentUserId = currentUserProvider.currentUserId().orElse(null);
        if (currentUserId == null || !currentUserId.equals(expectedUserId)) {
            throw exceptionFactory.apply(expectedUserId, currentUserId);
        }
    }

    public void assertCurrentUserHasRole(String roleName, Supplier<? extends RuntimeException> exceptionFactory) {
        if (!currentUserProvider.hasRole(roleName)) {
            throw exceptionFactory.get();
        }
    }
}
