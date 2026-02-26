package app.partsvibe.users.test.databuilders;

import app.partsvibe.users.domain.RoleNames;
import app.partsvibe.users.domain.invite.UserInvite;
import java.time.Instant;
import java.util.UUID;

public final class UserInviteTestDataBuilder {
    private String email = "invite-" + UUID.randomUUID() + "@example.com";
    private String roleName = RoleNames.USER;
    private String inviteMessage = null;
    private String tokenHash = UUID.randomUUID().toString().replace("-", "");
    private Instant expiresAt = Instant.parse("2099-01-01T00:00:00Z");
    private Instant usedAt = null;
    private Instant revokedAt = null;

    private UserInviteTestDataBuilder() {}

    public static UserInviteTestDataBuilder aUserInvite() {
        return new UserInviteTestDataBuilder();
    }

    public UserInviteTestDataBuilder withEmail(String email) {
        this.email = email;
        return this;
    }

    public UserInviteTestDataBuilder withRoleName(String roleName) {
        this.roleName = roleName;
        return this;
    }

    public UserInviteTestDataBuilder withInviteMessage(String inviteMessage) {
        this.inviteMessage = inviteMessage;
        return this;
    }

    public UserInviteTestDataBuilder withTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
        return this;
    }

    public UserInviteTestDataBuilder withExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public UserInviteTestDataBuilder withUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
        return this;
    }

    public UserInviteTestDataBuilder withRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
        return this;
    }

    public UserInvite build() {
        UserInvite invite = new UserInvite(email, roleName, inviteMessage, tokenHash, expiresAt);
        invite.setUsedAt(usedAt);
        invite.setRevokedAt(revokedAt);
        return invite;
    }
}
