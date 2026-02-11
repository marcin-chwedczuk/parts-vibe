package app.partsvibe.infra.events.jpa;

import java.time.Instant;
import java.util.List;

public interface EventQueueRepositoryCustom {
    List<ClaimedEventQueueEntry> claimBatchForProcessing(int batchSize, int maxAttempts, String workerId, Instant now);
}
