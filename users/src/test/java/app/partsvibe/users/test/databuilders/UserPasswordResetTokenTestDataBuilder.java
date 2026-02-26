package app.partsvibe.users.test.databuilders;

import app.partsvibe.users.domain.User;
import app.partsvibe.users.domain.security.UserPasswordResetToken;
import java.time.Instant;
import java.util.UUID;

public final class UserPasswordResetTokenTestDataBuilder {
    private User user = UserTestDataBuilder.aUser().build();
    private String tokenHash = UUID.randomUUID().toString().replace("-", "");
    private Instant expiresAt = Instant.parse("2099-01-01T00:00:00Z");
    private Instant usedAt = null;
    private Instant revokedAt = null;

    private UserPasswordResetTokenTestDataBuilder() {}

    public static UserPasswordResetTokenTestDataBuilder aUserPasswordResetToken() {
        return new UserPasswordResetTokenTestDataBuilder();
    }

    public UserPasswordResetTokenTestDataBuilder withUser(User user) {
        this.user = user;
        return this;
    }

    public UserPasswordResetTokenTestDataBuilder withTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
        return this;
    }

    public UserPasswordResetTokenTestDataBuilder withExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public UserPasswordResetTokenTestDataBuilder withUsedAt(Instant usedAt) {
        this.usedAt = usedAt;
        return this;
    }

    public UserPasswordResetTokenTestDataBuilder withRevokedAt(Instant revokedAt) {
        this.revokedAt = revokedAt;
        return this;
    }

    public UserPasswordResetToken build() {
        UserPasswordResetToken token = new UserPasswordResetToken(user, tokenHash, expiresAt);
        token.setUsedAt(usedAt);
        token.setRevokedAt(revokedAt);
        return token;
    }
}
