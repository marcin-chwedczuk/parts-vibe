package app.partsvibe.infra.events;

import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxEventClaimService {
    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventClaimService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public int requeueStaleProcessing(long processingTimeoutMs) {
        Instant now = Instant.now();
        Instant lockedBefore = now.minusMillis(processingTimeoutMs);
        return outboxEventRepository.requeueStaleProcessing(lockedBefore, now);
    }

    public List<ClaimedOutboxEvent> claimBatch(int batchSize, int maxAttempts, String workerId) {
        return outboxEventRepository.claimBatchForProcessing(batchSize, maxAttempts, workerId);
    }
}
