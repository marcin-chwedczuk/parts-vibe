package app.partsvibe.users.queries.auth;

import app.partsvibe.shared.cqrs.BaseQueryHandler;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.security.UserPrincipal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
class FindUserDetailsByUsernameQueryHandler extends BaseQueryHandler<FindUserDetailsByUsernameQuery, UserDetails> {
    private static final Logger log = LoggerFactory.getLogger(FindUserDetailsByUsernameQueryHandler.class);

    private final UserRepository userRepository;

    FindUserDetailsByUsernameQueryHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected UserDetails doHandle(FindUserDetailsByUsernameQuery query) {
        log.info("Loading user details for username={}", query.username());
        return userRepository
                .findByUsername(query.username())
                .map(UserPrincipal::new)
                .orElseThrow(() -> {
                    log.warn("User not found: {}", query.username());
                    return new UsernameNotFoundException("User not found: " + query.username());
                });
    }
}
