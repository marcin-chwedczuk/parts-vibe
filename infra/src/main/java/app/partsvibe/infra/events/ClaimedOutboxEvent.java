package app.partsvibe.infra.events;

import java.util.UUID;

public record ClaimedOutboxEvent(
        long id, UUID eventId, String eventType, String payload, String requestId, int attemptCount) {
    public static ClaimedOutboxEvent fromEntity(OutboxEventEntity entity) {
        return new ClaimedOutboxEvent(
                entity.getId(),
                entity.getEventId(),
                entity.getEventType(),
                entity.getPayload(),
                entity.getRequestId(),
                entity.getAttemptCount());
    }
}
