package app.partsvibe.infra.events.it.support;

import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.IntegrationEvent;
import java.util.UUID;

@IntegrationEvent(name = QueueAlwaysFailEvent.EVENT_NAME)
public record QueueAlwaysFailEvent(UUID eventId, String key) implements Event {
    public static final String EVENT_NAME = "queue_always_fail_event";

    public static QueueAlwaysFailEvent create(String key) {
        return new QueueAlwaysFailEvent(UUID.randomUUID(), key);
    }
}
