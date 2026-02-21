package app.partsvibe.storage.events;

import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.IntegrationEvent;
import app.partsvibe.storage.api.StorageObjectType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@IntegrationEvent(name = FileUploadedEvent.EVENT_NAME)
public record FileUploadedEvent(
        UUID eventId, Instant occurredAt, Optional<String> requestId, UUID fileId, StorageObjectType objectType)
        implements Event {
    public static final String EVENT_NAME = "file_uploaded";

    public static FileUploadedEvent create(
            UUID fileId, StorageObjectType objectType, String requestId, Instant occurredAt) {
        return new FileUploadedEvent(UUID.randomUUID(), occurredAt, Optional.ofNullable(requestId), fileId, objectType);
    }
}
