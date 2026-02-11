package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.events.jpa.ClaimedOutboxEvent;
import app.partsvibe.infra.events.jpa.OutboxEventRepository;
import app.partsvibe.shared.time.TimeProvider;
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
    private final TimeProvider timeProvider;

    public OutboxEventClaimService(OutboxEventRepository outboxEventRepository, TimeProvider timeProvider) {
        this.outboxEventRepository = outboxEventRepository;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public int requeueStaleProcessing(long processingTimeoutMs) {
        Instant now = timeProvider.now();
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
        Instant now = timeProvider.now();
        List<ClaimedOutboxEvent> claimed =
                outboxEventRepository.claimBatchForProcessing(batchSize, maxAttempts, workerId, now);
        log.debug(
                "Claim batch finished. workerId={}, batchSize={}, maxAttempts={}, claimedCount={}",
                workerId,
                batchSize,
                maxAttempts,
                claimed.size());
        return claimed;
    }
}
