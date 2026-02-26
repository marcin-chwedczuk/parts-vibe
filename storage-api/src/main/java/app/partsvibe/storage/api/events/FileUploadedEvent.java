package app.partsvibe.storage.api.events;

import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.IntegrationEvent;
import app.partsvibe.storage.api.StorageObjectType;
import java.util.UUID;
import lombok.Builder;

@IntegrationEvent(name = FileUploadedEvent.EVENT_NAME)
@Builder
public record FileUploadedEvent(UUID eventId, UUID fileId, StorageObjectType objectType) implements Event {
    public static final String EVENT_NAME = "file_uploaded";

    public FileUploadedEvent {
        if (eventId == null) {
            eventId = UUID.randomUUID();
        }
    }
}
