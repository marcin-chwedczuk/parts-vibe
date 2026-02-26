package app.partsvibe.users.test.databuilders;

import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.User;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class UserTestDataBuilder {
    private String username = "user-" + UUID.randomUUID();
    private String passwordHash = "noop";
    private boolean enabled = true;
    private UUID avatarId = null;
    private final List<Role> roles = new ArrayList<>();

    private UserTestDataBuilder() {}

    public static UserTestDataBuilder aUser() {
        return new UserTestDataBuilder();
    }

    public UserTestDataBuilder withUsername(String username) {
        this.username = username;
        return this;
    }

    public UserTestDataBuilder withPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
        return this;
    }

    public UserTestDataBuilder enabled() {
        this.enabled = true;
        return this;
    }

    public UserTestDataBuilder disabled() {
        this.enabled = false;
        return this;
    }

    public UserTestDataBuilder withRole(Role role) {
        this.roles.add(role);
        return this;
    }

    public UserTestDataBuilder withRoles(Role... roles) {
        this.roles.addAll(List.of(roles));
        return this;
    }

    public UserTestDataBuilder withAvatarId(UUID avatarId) {
        this.avatarId = avatarId;
        return this;
    }

    public User build() {
        User user = new User(username, passwordHash);
        user.setEnabled(enabled);
        user.setAvatarId(avatarId);
        user.getRoles().addAll(roles);
        return user;
    }
}
