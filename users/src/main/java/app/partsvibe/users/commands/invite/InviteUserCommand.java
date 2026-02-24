package app.partsvibe.users.commands.invite;

import app.partsvibe.shared.cqrs.Command;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record InviteUserCommand(
        @NotBlank @Email @Size(max = 64) String email,
        @NotBlank @Size(max = 32) String roleName,
        @Positive int validityHours,
        @Size(max = 1000) String inviteMessage)
        implements Command<InviteUserCommandResult> {}
