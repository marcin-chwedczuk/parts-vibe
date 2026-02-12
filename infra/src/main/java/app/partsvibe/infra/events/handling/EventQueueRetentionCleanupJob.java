package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.events.jpa.EventQueueRepository;
import app.partsvibe.shared.time.TimeProvider;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventQueueRetentionCleanupJob {
    private static final Logger log = LoggerFactory.getLogger(EventQueueRetentionCleanupJob.class);

    private final EventQueueDispatcherProperties properties;
    private final EventQueueRepository eventQueueRepository;
    private final TimeProvider timeProvider;

    public EventQueueRetentionCleanupJob(
            EventQueueDispatcherProperties properties,
            EventQueueRepository eventQueueRepository,
            TimeProvider timeProvider) {
        this.properties = properties;
        this.eventQueueRepository = eventQueueRepository;
        this.timeProvider = timeProvider;
    }

    @Scheduled(fixedDelayString = "${app.events.dispatcher.retention-cleanup-interval-ms:3600000}")
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

        // TODO: Do this in a loop
        int doneDeleted = eventQueueRepository.deleteDoneOlderThan(doneCutoff, batchSize);
        int failedDeleted = eventQueueRepository.deleteFailedOlderThan(failedCutoff, batchSize);

        log.info(
                "Event queue retention cleanup finished. doneDeleted={}, failedDeleted={}, doneCutoff={}, failedCutoff={}, batchSize={}",
                doneDeleted,
                failedDeleted,
                doneCutoff,
                failedCutoff,
                batchSize);
    }
}
