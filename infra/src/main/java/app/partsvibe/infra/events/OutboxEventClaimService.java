package app.partsvibe.infra.events;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OutboxEventClaimService {
    private static final Logger log = LoggerFactory.getLogger(OutboxEventClaimService.class);

    private final OutboxEventRepository outboxEventRepository;

    public OutboxEventClaimService(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Transactional
    public int requeueStaleProcessing(long processingTimeoutMs) {
        Instant now = Instant.now();
        Instant lockedBefore = now.minusMillis(processingTimeoutMs);
        int requeued = outboxEventRepository.requeueStaleProcessing(lockedBefore, now);
        log.debug(
                "Requeue stale processing evaluated. processingTimeoutMs={}, lockedBefore={}, requeued={}",
                processingTimeoutMs,
                lockedBefore,
                requeued);
        return requeued;
    }

    public List<ClaimedOutboxEvent> claimBatch(int batchSize, int maxAttempts, String workerId) {
        List<ClaimedOutboxEvent> claimed =
                outboxEventRepository.claimBatchForProcessing(batchSize, maxAttempts, workerId);
        log.debug(
                "Claim batch finished. workerId={}, batchSize={}, maxAttempts={}, claimedCount={}",
                workerId,
                batchSize,
                maxAttempts,
                claimed.size());
        return claimed;
    }
}
