package app.partsvibe.users.commands.profile;

import app.partsvibe.shared.cqrs.Command;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record UpdateCurrentUserAvatarCommand(@NotBlank String username, @NotNull UUID avatarId)
        implements Command<UpdateCurrentUserAvatarCommandResult> {}
