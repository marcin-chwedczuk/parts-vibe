package app.partsvibe.infra.events.jpa;

import java.time.Instant;
import java.util.UUID;

public record ClaimedEventQueueEntry(
        long id,
        UUID eventId,
        String eventName,
        int schemaVersion,
        Instant publishedAt,
        String payload,
        String requestId,
        String publishedBy,
        int attemptCount) {
    public static ClaimedEventQueueEntry fromEntity(EventQueueEntry entity) {
        return new ClaimedEventQueueEntry(
                entity.getId(),
                entity.getEventId(),
                entity.getEventName(),
                entity.getSchemaVersion(),
                entity.getPublishedAt(),
                entity.getPayload(),
                entity.getRequestId(),
                entity.getPublishedBy(),
                entity.getAttemptCount());
    }

    public String toStringWithoutPayload() {
        return "ClaimedEventQueueEntry{id=%d, eventId=%s, eventName='%s', schemaVersion=%d, publishedAt=%s, requestId='%s', publishedBy='%s', attemptCount=%d}"
                .formatted(id, eventId, eventName, schemaVersion, publishedAt, requestId, publishedBy, attemptCount);
    }
}
