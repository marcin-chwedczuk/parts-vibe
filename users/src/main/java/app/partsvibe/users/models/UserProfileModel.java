package app.partsvibe.users.models;

import java.util.UUID;

public record UserProfileModel(Long id, String username, String bio, String website, UUID avatarId) {}
