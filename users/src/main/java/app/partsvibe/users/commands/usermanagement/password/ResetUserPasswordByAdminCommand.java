package app.partsvibe.users.commands.usermanagement.password;

import app.partsvibe.shared.cqrs.Command;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record ResetUserPasswordByAdminCommand(
        @NotNull @Positive Long targetUserId,
        @NotNull @Positive Long adminUserId,
        @NotBlank @Size(max = 512) String adminPassword)
        implements Command<ResetUserPasswordByAdminCommandResult> {}
