package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.events.jpa.EventQueueRepository;
import app.partsvibe.shared.time.TimeProvider;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventQueueStaleRecoveryJob {
    private static final Logger log = LoggerFactory.getLogger(EventQueueStaleRecoveryJob.class);

    private final EventQueueDispatcherProperties properties;
    private final EventQueueRepository eventQueueRepository;
    private final TimeProvider timeProvider;

    public EventQueueStaleRecoveryJob(
            EventQueueDispatcherProperties properties,
            EventQueueRepository eventQueueRepository,
            TimeProvider timeProvider) {
        this.properties = properties;
        this.eventQueueRepository = eventQueueRepository;
        this.timeProvider = timeProvider;
    }

    @Scheduled(fixedDelayString = "${app.events.dispatcher.stale-recovery-interval-ms:60000}")
    public void requeueStaleProcessing() {
        if (!properties.isEnabled()) {
            return;
        }

        Instant now = timeProvider.now();
        Instant lockedBefore = now.minusMillis(properties.getProcessingTimeoutMs());
        // TODO: Respect max attempts, use batch size
        int requeued = eventQueueRepository.requeueStaleProcessing(lockedBefore, now);
        if (requeued > 0) {
            log.warn(
                    "Recovered stale PROCESSING event queue entries. count={}, processingTimeoutMs={}, lockedBefore={}",
                    requeued,
                    properties.getProcessingTimeoutMs(),
                    lockedBefore);
        } else {
            log.debug("No stale PROCESSING event queue entries found");
        }
    }
}
