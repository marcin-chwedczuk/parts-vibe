package app.partsvibe.infra.events.jpa;

import java.time.Instant;
import java.util.List;

public interface EventQueueRepository {
    EventQueueEntry save(EventQueueEntry entry);

    int markEntryAsDone(long id, Instant now);
    int markEntryAsFailed(long id, Instant nextAttemptAt, String lastError, Instant now);

    int recoverTimedOutProcessingEntries(Instant lockedBefore, Instant now);

    int deleteEntriesByStatusOlderThan(EventQueueEntryStatus status, Instant cutoff, int limit);

    List<ClaimedEventQueueEntry> claimEntriesForProcessing(int batchSize, int maxAttempts, String workerId, Instant now);
    int releaseClaimedEntry(long id, Instant nextAttemptAt, Instant now);
}
