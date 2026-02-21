package app.partsvibe.users.commands.profile;

import java.util.UUID;

public record UpdateCurrentUserAvatarCommandResult(UUID previousAvatarId, UUID currentAvatarId) {}
