package app.partsvibe.infra.events.jpa;

import java.util.UUID;

public record ClaimedEventQueueEntry(
        long id, UUID eventId, String eventType, String payload, String requestId, int attemptCount) {
    public static ClaimedEventQueueEntry fromEntity(EventQueueEntry entity) {
        return new ClaimedEventQueueEntry(
                entity.getId(),
                entity.getEventId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getRequestId(),
                entity.getAttemptCount());
    }
}
