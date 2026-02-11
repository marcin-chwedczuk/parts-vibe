package app.partsvibe.shared.events.model;

import java.time.Instant;
import java.util.UUID;

public interface Event {
    UUID eventId();

    Instant occurredAt();

    String requestId();

    String eventType();

    int schemaVersion();
}
