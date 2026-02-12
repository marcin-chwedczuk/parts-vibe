package app.partsvibe.infra.events.jpa;

import java.util.UUID;

public record ClaimedEventQueueEntry(
        long id,
        UUID eventId,
        String eventType,
        int schemaVersion,
        String payload,
        String requestId,
        int attemptCount) {
    public static ClaimedEventQueueEntry fromEntity(EventQueueEntry entity) {
        return new ClaimedEventQueueEntry(
                entity.getId(),
                entity.getEventId(),
                entity.getEventType(),
                entity.getSchemaVersion(),
                entity.getPayload(),
                entity.getRequestId(),
                entity.getAttemptCount());
    }

    public String toStringWithoutPayload() {
        return "ClaimedEventQueueEntry{id=%d, eventId=%s, eventType='%s', schemaVersion=%d, requestId='%s', attemptCount=%d}"
                .formatted(id, eventId, eventType, schemaVersion, requestId, attemptCount);
    }
}
