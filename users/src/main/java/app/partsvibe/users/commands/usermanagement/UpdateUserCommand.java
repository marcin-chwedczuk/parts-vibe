package app.partsvibe.users.commands.usermanagement;

import app.partsvibe.shared.cqrs.Command;
import app.partsvibe.users.models.UserDetailsModel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record UpdateUserCommand(
        @Positive Long userId, @NotBlank @Email @Size(max = 64) String username, boolean enabled)
        implements Command<UserDetailsModel> {}
