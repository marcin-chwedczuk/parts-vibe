package app.partsvibe.users.events;

import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.IntegrationEvent;
import java.time.Instant;
import java.util.UUID;

@IntegrationEvent(name = PasswordResetRequestedEvent.EVENT_NAME)
public record PasswordResetRequestedEvent(UUID eventId, String email, String token, Instant expiresAt)
        implements Event {
    public static final String EVENT_NAME = "password_reset_requested";

    public static PasswordResetRequestedEvent create(String email, String token, Instant expiresAt) {
        return new PasswordResetRequestedEvent(UUID.randomUUID(), email, token, expiresAt);
    }
}
