package app.partsvibe.site.events;

import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.EventDescriptor;
import java.time.Instant;
import java.util.UUID;

@EventDescriptor(name = TestEvent.EVENT_TYPE)
public record TestEvent(UUID eventId, Instant occurredAt, String requestId, String message) implements Event {
    public static final String EVENT_TYPE = "test_event";

    public static TestEvent create(String requestId, String message) {
        return new TestEvent(UUID.randomUUID(), Instant.now(), requestId, message);
    }
}
