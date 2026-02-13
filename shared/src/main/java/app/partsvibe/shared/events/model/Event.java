package app.partsvibe.shared.events.model;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

// TODO: Move those fields to EventMetadata should should be set by EventPublisher
// Event consumers should get handle(Event e, EventMetadata metadata)
public interface Event {
    UUID eventId();

    Instant occurredAt();

    Optional<String> requestId();
}
