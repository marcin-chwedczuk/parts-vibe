package app.partsvibe.shared.events.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import lombok.Builder;

@Builder(toBuilder = true)
public record EventMetadata(
        String eventName, int schemaVersion, UUID eventId, String requestId, Instant publishedAt, String publishedBy) {
    public String toLogString() {
        return "EventMetadata{eventName='%s', schemaVersion=%d, eventId=%s, requestId='%s', publishedAt=%s, publishedBy='%s'}"
                .formatted(
                        eventName,
                        schemaVersion,
                        eventId,
                        requestId == null ? "<none>" : requestId,
                        publishedAt,
                        publishedBy == null ? "<none>" : publishedBy);
    }

    @Override
    public String toString() {
        return toLogString();
    }

    public static EventMetadata fromEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event must not be null.");
        }
        var schema = fromEventClass(event.getClass());
        return schema.withEventId(event.eventId());
    }

    public static EventMetadata fromEventClass(Class<? extends Event> eventClass) {
        Objects.requireNonNull(eventClass, "Event class must not be null.");
        IntegrationEvent annotation = eventClass.getAnnotation(IntegrationEvent.class);
        if (annotation == null || annotation.name().isBlank()) {
            throw new IllegalStateException(
                    "Event class must be annotated with @IntegrationEvent and non-blank name: " + eventClass.getName());
        }
        if (annotation.version() <= 0) {
            throw new IllegalStateException(
                    "Event schemaVersion must be greater than 0. eventClass=%s, schemaVersion=%d"
                            .formatted(eventClass.getName(), annotation.version()));
        }
        return schema(annotation.name(), annotation.version());
    }

    public static EventMetadata schema(String eventName, int schemaVersion) {
        return EventMetadata.builder()
                .eventName(eventName)
                .schemaVersion(schemaVersion)
                .build();
    }

    public EventMetadata withEventId(UUID eventId) {
        return toBuilder().eventId(eventId).build();
    }

    public EventMetadata withInfrastructureContext(String requestId, Instant publishedAt, String publishedBy) {
        return toBuilder()
                .requestId(requestId)
                .publishedAt(publishedAt)
                .publishedBy(publishedBy)
                .build();
    }
}
