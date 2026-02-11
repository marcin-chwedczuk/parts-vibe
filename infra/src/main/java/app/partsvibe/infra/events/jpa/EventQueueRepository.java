package app.partsvibe.infra.events.jpa;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventQueueRepository {
    EventQueueEntry save(EventQueueEntry entry);

    Optional<EventQueueEntry> findByEventId(UUID eventId);

    int markDone(long id, Instant now);

    int markFailed(long id, Instant nextAttemptAt, String lastError, Instant now);

    int requeueStaleProcessing(Instant lockedBefore, Instant now);

    List<ClaimedEventQueueEntry> claimBatchForProcessing(int batchSize, int maxAttempts, String workerId, Instant now);
}
