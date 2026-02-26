package app.partsvibe.users.test.databuilders;

import app.partsvibe.users.domain.User;
import app.partsvibe.users.domain.security.UserPasswordResetToken;
import java.time.Instant;
import java.util.UUID;

public final class UserPasswordResetTokenTestDataBuilder {
    private User user = UserTestDataBuilder.aUser().build();
    private String tokenHash = UUID.randomUUID().toString().replace("-", "");
    private Instant expiresAt = Instant.parse("2099-01-01T00:00:00Z");

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

    public UserPasswordResetToken build() {
        return new UserPasswordResetToken(user, tokenHash, expiresAt);
    }
}
