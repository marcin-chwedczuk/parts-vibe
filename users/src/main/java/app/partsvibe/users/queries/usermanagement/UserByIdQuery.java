package app.partsvibe.users.queries.usermanagement;

import app.partsvibe.shared.cqrs.Query;
import app.partsvibe.users.models.UserDetailsModel;
import jakarta.validation.constraints.Positive;

public record UserByIdQuery(@Positive Long userId) implements Query<UserDetailsModel> {}
