package app.partsvibe.users.queries.profile;

import app.partsvibe.shared.cqrs.Query;
import app.partsvibe.users.models.UserProfileModel;
import jakarta.validation.constraints.NotBlank;

public record GetCurrentUserProfileQuery(@NotBlank String username) implements Query<UserProfileModel> {}
