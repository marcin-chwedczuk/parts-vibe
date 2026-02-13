package app.partsvibe.shared.events.admin;

import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.IntegrationEvent;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@IntegrationEvent(name = RetentionCleanupRequestedEvent.EVENT_NAME)
public record RetentionCleanupRequestedEvent(UUID eventId, Instant occurredAt, Optional<String> requestId)
        implements Event {
    public static final String EVENT_NAME = "retention_cleanup_requested";

    public static RetentionCleanupRequestedEvent create(String requestId, Instant occurredAt) {
        return new RetentionCleanupRequestedEvent(UUID.randomUUID(), occurredAt, Optional.ofNullable(requestId));
    }
}
