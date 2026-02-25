package app.partsvibe.users.commands.invite;

import app.partsvibe.shared.cqrs.Command;
import app.partsvibe.shared.cqrs.NoResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FinalizeUserInviteCommand(
        @NotBlank @Size(max = 256) String token,
        @NotBlank @Size(min = 12, max = 512) String password,
        @NotBlank @Size(min = 12, max = 512) String repeatedPassword)
        implements Command<NoResult> {}
