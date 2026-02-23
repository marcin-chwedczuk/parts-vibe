package app.partsvibe.shared.events.admin;

import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.IntegrationEvent;
import java.util.UUID;

@IntegrationEvent(name = RetentionCleanupRequestedEvent.EVENT_NAME)
public record RetentionCleanupRequestedEvent(UUID eventId) implements Event {
    public static final String EVENT_NAME = "retention_cleanup_requested";

    public static RetentionCleanupRequestedEvent create() {
        return new RetentionCleanupRequestedEvent(UUID.randomUUID());
    }
}
