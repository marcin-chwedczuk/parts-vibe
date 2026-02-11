package app.partsvibe.site.events;

import app.partsvibe.shared.events.Event;
import app.partsvibe.shared.events.EventTypeName;
import java.time.Instant;
import java.util.UUID;

@EventTypeName(TestEvent.EVENT_TYPE)
public record TestEvent(UUID eventId, Instant occurredAt, String requestId, String message) implements Event {
    public static final String EVENT_TYPE = "test_event";

    public static TestEvent create(String requestId, String message) {
        return new TestEvent(UUID.randomUUID(), Instant.now(), requestId, message);
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }

    @Override
    public int schemaVersion() {
        return 1;
    }
}
