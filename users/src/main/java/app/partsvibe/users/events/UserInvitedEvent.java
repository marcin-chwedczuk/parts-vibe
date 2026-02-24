package app.partsvibe.users.events;

import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.IntegrationEvent;
import java.time.Instant;
import java.util.UUID;

@IntegrationEvent(name = UserInvitedEvent.EVENT_NAME)
public record UserInvitedEvent(
        UUID eventId, String email, String token, Instant expiresAt, String inviteMessage, String invitedRole)
        implements Event {
    public static final String EVENT_NAME = "user_invited";

    public static UserInvitedEvent create(
            String email, String token, Instant expiresAt, String inviteMessage, String invitedRole) {
        return new UserInvitedEvent(UUID.randomUUID(), email, token, expiresAt, inviteMessage, invitedRole);
    }
}
