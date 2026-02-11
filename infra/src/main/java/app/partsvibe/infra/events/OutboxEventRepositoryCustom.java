package app.partsvibe.infra.events;

import java.util.List;

public interface OutboxEventRepositoryCustom {
    List<ClaimedOutboxEvent> claimBatchForProcessing(int batchSize, int maxAttempts, String workerId);
}
