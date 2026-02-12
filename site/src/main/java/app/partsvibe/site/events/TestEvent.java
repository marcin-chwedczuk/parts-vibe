package app.partsvibe.site.events;

import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.PublishableEvent;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@PublishableEvent(name = TestEvent.EVENT_NAME)
public record TestEvent(UUID eventId, Instant occurredAt, Optional<String> requestId, String message) implements Event {
    public static final String EVENT_NAME = "test_event";

    public static TestEvent create(String requestId, String message) {
        return new TestEvent(UUID.randomUUID(), Instant.now(), Optional.ofNullable(requestId), message);
    }
}
