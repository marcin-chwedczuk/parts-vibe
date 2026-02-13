package app.partsvibe.users.queries.auth;

import app.partsvibe.shared.cqrs.Query;
import org.springframework.security.core.userdetails.UserDetails;

public record FindUserDetailsByUsernameQuery(String username) implements Query<UserDetails> {}
