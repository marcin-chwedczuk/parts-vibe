package app.partsvibe.users.queries.auth;

import app.partsvibe.shared.cqrs.Query;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

public record GetUserMenuQuery(@NotNull @Positive Long userId) implements Query<GetUserMenuQuery.UserMenuData> {
    public record UserMenuData(UUID avatarId) {}
}
