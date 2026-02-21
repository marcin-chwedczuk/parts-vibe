package app.partsvibe.storage.events;

import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.IntegrationEvent;
import app.partsvibe.storage.api.StorageObjectType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@IntegrationEvent(name = FileReadyEvent.EVENT_NAME)
public record FileReadyEvent(
        UUID eventId, Instant occurredAt, Optional<String> requestId, UUID fileId, StorageObjectType objectType)
        implements Event {
    public static final String EVENT_NAME = "file_ready";

    public static FileReadyEvent create(
            UUID fileId, StorageObjectType objectType, String requestId, Instant occurredAt) {
        return new FileReadyEvent(UUID.randomUUID(), occurredAt, Optional.ofNullable(requestId), fileId, objectType);
    }
}
