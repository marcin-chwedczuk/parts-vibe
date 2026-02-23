package app.partsvibe.users.queries.profile;

import app.partsvibe.shared.cqrs.Query;
import app.partsvibe.users.models.UserProfileModel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GetCurrentUserProfileQuery(@NotNull @Positive Long userId) implements Query<UserProfileModel> {}
