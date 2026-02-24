package app.partsvibe.users.commands.password;

import app.partsvibe.shared.cqrs.Command;
import app.partsvibe.shared.cqrs.NoResult;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RequestPasswordResetCommand(@NotBlank @Email @Size(max = 64) String email) implements Command<NoResult> {}
