package app.partsvibe.users.security;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.users.queries.auth.FindUserDetailsByUsernameQuery;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Service
public class JpaUserDetailsService implements UserDetailsService {
    private final Mediator mediator;

    public JpaUserDetailsService(Mediator mediator) {
        this.mediator = mediator;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return mediator.executeQuery(new FindUserDetailsByUsernameQuery(username));
    }
}
