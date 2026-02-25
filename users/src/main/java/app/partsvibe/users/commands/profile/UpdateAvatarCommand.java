package app.partsvibe.users.commands.profile;

import app.partsvibe.shared.cqrs.Command;
import app.partsvibe.shared.cqrs.NoResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateAvatarCommand(
        @NotNull @Positive Long userId,
        @NotBlank @Size(max = 256) String originalFilename,
        @NotNull @Size(min = 1) byte[] content)
        implements Command<NoResult> {}
