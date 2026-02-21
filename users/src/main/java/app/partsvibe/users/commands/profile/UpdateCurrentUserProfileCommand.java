package app.partsvibe.users.commands.profile;

import app.partsvibe.shared.cqrs.Command;
import app.partsvibe.shared.cqrs.NoResult;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCurrentUserProfileCommand(
        @NotBlank String username,
        @Size(max = 1000, message = "{profile.validation.bio.max}") String bio,
        @Size(max = 255, message = "{profile.validation.website.max}") String website)
        implements Command<NoResult> {}
