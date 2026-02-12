package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.events.jpa.EventQueueRepository;
import app.partsvibe.shared.time.TimeProvider;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventQueueTimedOutProcessingRecoveryJob {
    private static final Logger log = LoggerFactory.getLogger(EventQueueTimedOutProcessingRecoveryJob.class);

    private final EventQueueDispatcherProperties properties;
    private final EventQueueRepository eventQueueRepository;
    private final TimeProvider timeProvider;

    public EventQueueTimedOutProcessingRecoveryJob(
            EventQueueDispatcherProperties properties,
            EventQueueRepository eventQueueRepository,
            TimeProvider timeProvider) {
        this.properties = properties;
        this.eventQueueRepository = eventQueueRepository;
        this.timeProvider = timeProvider;
    }

    @Scheduled(fixedDelayString = "${app.events.dispatcher.stale-recovery-interval-ms:60000}")
    public void recoverTimedOutProcessing() {
        if (!properties.isEnabled()) {
            log.warn("EventQueueTimedOutProcessingRecoveryJob is disabled.");
            return;
        }

        Instant now = timeProvider.now();
        Instant lockedBefore = now.minusMillis(properties.getProcessingTimeoutMs());

        int recovered = eventQueueRepository.recoverTimedOutProcessingEntries(lockedBefore, now);
        if (recovered > 0) {
            log.warn(
                    "Recovered timed-out PROCESSING event queue entries. count={}, processingTimeoutMs={}, lockedBefore={}",
                    recovered,
                    properties.getProcessingTimeoutMs(),
                    lockedBefore);
        }
    }
}
