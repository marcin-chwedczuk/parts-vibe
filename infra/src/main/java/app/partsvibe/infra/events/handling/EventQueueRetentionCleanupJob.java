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
public class EventQueueRetentionCleanupJob {
    private static final Logger log = LoggerFactory.getLogger(EventQueueRetentionCleanupJob.class);

    private final EventQueueWorkerProperties properties;
    private final EventQueueRepository eventQueueRepository;
    private final TimeProvider timeProvider;
    private final Counter doneDeletedCounter;
    private final Counter failedDeletedCounter;

    public EventQueueRetentionCleanupJob(
            EventQueueWorkerProperties properties,
            EventQueueRepository eventQueueRepository,
            TimeProvider timeProvider,
            MeterRegistry meterRegistry) {
        this.properties = properties;
        this.eventQueueRepository = eventQueueRepository;
        this.timeProvider = timeProvider;
        this.doneDeletedCounter = meterRegistry.counter("app.events.worker.retention.done.deleted");
        this.failedDeletedCounter = meterRegistry.counter("app.events.worker.retention.failed.deleted");
    }

    @Scheduled(fixedDelayString = "${app.events.worker.retention-cleanup-interval-ms:3600000}")
    public void cleanup() {
        if (!properties.isEnabled()) {
            return;
        }

        int batchSize = properties.getRetentionDeleteBatchSize();
        if (batchSize <= 0) {
            log.debug("Skipping retention cleanup because retentionDeleteBatchSize <= 0");
            return;
        }

        Instant now = timeProvider.now();
        Instant doneCutoff = now.minusSeconds((long) properties.getDoneRetentionDays() * 24 * 60 * 60);
        Instant failedCutoff = now.minusSeconds((long) properties.getFailedRetentionDays() * 24 * 60 * 60);

        int doneDeleted = eventQueueRepository.deleteDoneOlderThan(doneCutoff, batchSize);
        int failedDeleted = eventQueueRepository.deleteFailedOlderThan(failedCutoff, batchSize);

        if (doneDeleted > 0) {
            doneDeletedCounter.increment(doneDeleted);
        }
        if (failedDeleted > 0) {
            failedDeletedCounter.increment(failedDeleted);
        }

        log.info(
                "Event queue retention cleanup finished. doneDeleted={}, failedDeleted={}, doneCutoff={}, failedCutoff={}, batchSize={}",
                doneDeleted,
                failedDeleted,
                doneCutoff,
                failedCutoff,
                batchSize);
    }
}
