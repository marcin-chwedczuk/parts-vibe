package app.partsvibe.users.commands.profile;

import app.partsvibe.shared.cqrs.Command;
import app.partsvibe.shared.cqrs.NoResult;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateCurrentUserProfileCommand(
        @NotNull @Positive Long userId,
        @Size(max = 1000, message = "{profile.validation.bio.max}") String bio,
        @Size(max = 255, message = "{profile.validation.website.max}") String website)
        implements Command<NoResult> {}
