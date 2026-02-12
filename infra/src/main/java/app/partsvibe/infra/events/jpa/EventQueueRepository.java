package app.partsvibe.infra.events.jpa;

import java.time.Instant;
import java.util.List;

public interface EventQueueRepository {
    EventQueueEntry save(EventQueueEntry entry);

    int markDone(long id, Instant now);

    int markFailed(long id, Instant nextAttemptAt, String lastError, Instant now);

    int releaseForRetry(long id, Instant nextAttemptAt, Instant now);

    int recoverTimedOutProcessing(Instant lockedBefore, Instant now);

    int deleteByStatusOlderThan(EventQueueStatus status, Instant cutoff, int limit);

    List<ClaimedEventQueueEntry> claimBatchForProcessing(int batchSize, int maxAttempts, String workerId, Instant now);
}
