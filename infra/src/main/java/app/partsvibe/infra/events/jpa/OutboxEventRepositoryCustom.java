package app.partsvibe.infra.events.jpa;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepositoryCustom {
    List<ClaimedOutboxEvent> claimBatchForProcessing(int batchSize, int maxAttempts, String workerId, Instant now);
}
