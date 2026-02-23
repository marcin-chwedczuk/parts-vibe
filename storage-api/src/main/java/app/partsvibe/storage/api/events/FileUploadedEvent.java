package app.partsvibe.storage.api.events;

import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.IntegrationEvent;
import app.partsvibe.storage.api.StorageObjectType;
import java.util.UUID;

@IntegrationEvent(name = FileUploadedEvent.EVENT_NAME)
public record FileUploadedEvent(UUID eventId, UUID fileId, StorageObjectType objectType) implements Event {
    public static final String EVENT_NAME = "file_uploaded";

    public static FileUploadedEvent create(UUID fileId, StorageObjectType objectType) {
        return new FileUploadedEvent(UUID.randomUUID(), fileId, objectType);
    }
}
