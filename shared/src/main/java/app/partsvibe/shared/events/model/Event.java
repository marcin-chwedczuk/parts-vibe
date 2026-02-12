package app.partsvibe.shared.events.model;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface Event {
    UUID eventId();

    Instant occurredAt();

    Optional<String> requestId();
}
