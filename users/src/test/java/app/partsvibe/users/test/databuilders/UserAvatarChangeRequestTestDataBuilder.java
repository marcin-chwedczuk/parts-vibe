package app.partsvibe.users.test.databuilders;

import app.partsvibe.users.domain.User;
import app.partsvibe.users.domain.avatar.UserAvatarChangeRequest;
import app.partsvibe.users.domain.avatar.UserAvatarChangeRequestStatus;
import java.time.Instant;
import java.util.UUID;

public final class UserAvatarChangeRequestTestDataBuilder {
    private User user = UserTestDataBuilder.aUser().build();
    private UUID newAvatarFileId = UUID.randomUUID();
    private UUID previousAvatarFileId = null;
    private Instant requestedAt = Instant.parse("2099-01-01T00:00:00Z");
    private UserAvatarChangeRequestStatus status = UserAvatarChangeRequestStatus.PENDING;
    private Instant resolvedAt = null;

    private UserAvatarChangeRequestTestDataBuilder() {}

    public static UserAvatarChangeRequestTestDataBuilder aUserAvatarChangeRequest() {
        return new UserAvatarChangeRequestTestDataBuilder();
    }

    public UserAvatarChangeRequestTestDataBuilder withUser(User user) {
        this.user = user;
        return this;
    }

    public UserAvatarChangeRequestTestDataBuilder withNewAvatarFileId(UUID newAvatarFileId) {
        this.newAvatarFileId = newAvatarFileId;
        return this;
    }

    public UserAvatarChangeRequestTestDataBuilder withPreviousAvatarFileId(UUID previousAvatarFileId) {
        this.previousAvatarFileId = previousAvatarFileId;
        return this;
    }

    public UserAvatarChangeRequestTestDataBuilder withRequestedAt(Instant requestedAt) {
        this.requestedAt = requestedAt;
        return this;
    }

    public UserAvatarChangeRequestTestDataBuilder withStatus(UserAvatarChangeRequestStatus status) {
        this.status = status;
        return this;
    }

    public UserAvatarChangeRequestTestDataBuilder withResolvedAt(Instant resolvedAt) {
        this.resolvedAt = resolvedAt;
        return this;
    }

    public UserAvatarChangeRequest build() {
        UserAvatarChangeRequest request =
                new UserAvatarChangeRequest(user, newAvatarFileId, previousAvatarFileId, requestedAt);
        request.setStatus(status);
        request.setResolvedAt(resolvedAt);
        return request;
    }
}
