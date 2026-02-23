package app.partsvibe.storage.api.events;

import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.IntegrationEvent;
import app.partsvibe.storage.api.StorageObjectType;
import java.util.UUID;

@IntegrationEvent(name = FileReadyEvent.EVENT_NAME)
public record FileReadyEvent(UUID eventId, UUID fileId, StorageObjectType objectType) implements Event {
    public static final String EVENT_NAME = "file_ready";

    public static FileReadyEvent create(UUID fileId, StorageObjectType objectType) {
        return new FileReadyEvent(UUID.randomUUID(), fileId, objectType);
    }
}
