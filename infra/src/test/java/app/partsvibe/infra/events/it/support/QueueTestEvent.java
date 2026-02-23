package app.partsvibe.infra.events.it.support;

import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.IntegrationEvent;
import java.util.UUID;

@IntegrationEvent(name = QueueTestEvent.EVENT_NAME)
public record QueueTestEvent(UUID eventId, String key, int failAttempts, long processingDelayMs) implements Event {
    public static final String EVENT_NAME = "queue_test_event";

    public static QueueTestEvent create(String key, int failAttempts, long processingDelayMs) {
        return new QueueTestEvent(UUID.randomUUID(), key, failAttempts, processingDelayMs);
    }
}
