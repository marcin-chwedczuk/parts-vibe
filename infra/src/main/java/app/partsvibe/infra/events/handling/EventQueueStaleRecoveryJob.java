package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.events.jpa.EventQueueRepository;
import app.partsvibe.shared.time.TimeProvider;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
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
    private final Counter staleRequeuedCounter;

    public EventQueueStaleRecoveryJob(
            EventQueueDispatcherProperties properties,
            EventQueueRepository eventQueueRepository,
            TimeProvider timeProvider,
            MeterRegistry meterRegistry) {
        this.properties = properties;
        this.eventQueueRepository = eventQueueRepository;
        this.timeProvider = timeProvider;
        this.staleRequeuedCounter = meterRegistry.counter("app.events.worker.stale.requeued");
    }

    @Scheduled(fixedDelayString = "${app.events.dispatcher.stale-recovery-interval-ms:60000}")
    public void requeueStaleProcessing() {
        if (!properties.isEnabled()) {
            return;
        }

        Instant now = timeProvider.now();
        Instant lockedBefore = now.minusMillis(properties.getProcessingTimeoutMs());
        int requeued = eventQueueRepository.requeueStaleProcessing(lockedBefore, now);
        if (requeued > 0) {
            staleRequeuedCounter.increment(requeued);
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
