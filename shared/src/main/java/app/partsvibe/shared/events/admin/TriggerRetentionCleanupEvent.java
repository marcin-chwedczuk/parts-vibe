package app.partsvibe.shared.events.admin;

import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.PublishableEvent;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@PublishableEvent(name = TriggerRetentionCleanupEvent.EVENT_NAME)
public record TriggerRetentionCleanupEvent(UUID eventId, Instant occurredAt, Optional<String> requestId)
        implements Event {
    public static final String EVENT_NAME = "trigger_retention_cleanup";

    public static TriggerRetentionCleanupEvent create(String requestId) {
        return new TriggerRetentionCleanupEvent(UUID.randomUUID(), Instant.now(), Optional.ofNullable(requestId));
    }
}
