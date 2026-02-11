package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.events.jpa.ClaimedEventQueueEntry;
import app.partsvibe.infra.events.jpa.EventQueueRepository;
import app.partsvibe.shared.time.TimeProvider;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventQueueClaimService {
    private static final Logger log = LoggerFactory.getLogger(EventQueueClaimService.class);

    private final EventQueueRepository eventQueueRepository;
    private final TimeProvider timeProvider;

    public EventQueueClaimService(EventQueueRepository eventQueueRepository, TimeProvider timeProvider) {
        this.eventQueueRepository = eventQueueRepository;
        this.timeProvider = timeProvider;
    }

    @Transactional
    public int requeueStaleProcessing(long processingTimeoutMs) {
        Instant now = timeProvider.now();
        Instant lockedBefore = now.minusMillis(processingTimeoutMs);
        int requeued = eventQueueRepository.requeueStaleProcessing(lockedBefore, now);
        log.debug(
                "Requeue stale processing evaluated. processingTimeoutMs={}, lockedBefore={}, requeued={}",
                processingTimeoutMs,
                lockedBefore,
                requeued);
        return requeued;
    }

    public List<ClaimedEventQueueEntry> claimBatch(int batchSize, int maxAttempts, String workerId) {
        Instant now = timeProvider.now();
        List<ClaimedEventQueueEntry> claimed =
                eventQueueRepository.claimBatchForProcessing(batchSize, maxAttempts, workerId, now);
        log.debug(
                "Claim batch finished. workerId={}, batchSize={}, maxAttempts={}, claimedCount={}",
                workerId,
                batchSize,
                maxAttempts,
                claimed.size());
        return claimed;
    }
}
