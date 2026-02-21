package app.partsvibe.users.queries.auth;

import app.partsvibe.shared.cqrs.Query;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record GetUserMenuQuery(@NotBlank String username) implements Query<GetUserMenuQuery.UserMenuData> {
    public record UserMenuData(String username, UUID avatarId) {}
}
