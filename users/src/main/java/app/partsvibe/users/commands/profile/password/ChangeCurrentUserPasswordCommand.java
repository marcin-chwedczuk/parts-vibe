package app.partsvibe.users.commands.profile.password;

import app.partsvibe.shared.cqrs.Command;
import app.partsvibe.shared.cqrs.NoResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ChangeCurrentUserPasswordCommand(
        @NotNull @Positive Long userId,
        @NotBlank @Size(max = 512) String currentPassword,
        @NotBlank @Size(min = 12, max = 512) String newPassword,
        @NotBlank @Size(min = 12, max = 512) String repeatedNewPassword)
        implements Command<NoResult> {}
