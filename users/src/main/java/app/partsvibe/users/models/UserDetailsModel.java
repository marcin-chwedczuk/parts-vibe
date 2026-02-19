package app.partsvibe.users.models;

import java.util.List;

public record UserDetailsModel(Long id, String username, boolean enabled, List<String> roles) {}
