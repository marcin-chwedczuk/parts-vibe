package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.events.jpa.EventQueueEntryStatus;
import app.partsvibe.infra.events.jpa.EventQueueRepository;
import app.partsvibe.shared.time.TimeProvider;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EventQueueRetentionCleanupService {
    private static final Logger log = LoggerFactory.getLogger(EventQueueRetentionCleanupService.class);
    private static final Duration DELETE_BATCH_PAUSE = Duration.ofMillis(50);
    private static final int DEFAULT_RETENTION_DELETE_BATCH_SIZE = 500;

    private final EventQueueDispatcherProperties properties;
    private final EventQueueRepository eventQueueRepository;
    private final TimeProvider timeProvider;

    public EventQueueRetentionCleanupService(
            EventQueueDispatcherProperties properties,
            EventQueueRepository eventQueueRepository,
            TimeProvider timeProvider) {
        this.properties = properties;
        this.eventQueueRepository = eventQueueRepository;
        this.timeProvider = timeProvider;
    }

    public RetentionCleanupResult cleanup(String trigger) {
        if (!properties.isEnabled()) {
            log.warn("Event queue retention cleanup skipped because dispatcher is disabled. trigger={}", trigger);
            return new RetentionCleanupResult(0, 0, 0, false);
        }

        var configuredBatchSize = properties.getRetentionDeleteBatchSize();
        var batchSize = configuredBatchSize > 0 ? configuredBatchSize : DEFAULT_RETENTION_DELETE_BATCH_SIZE;
        if (configuredBatchSize <= 0) {
            log.warn(
                    "Invalid retentionDeleteBatchSize configured ({}). Falling back to default batchSize={}.",
                    configuredBatchSize,
                    DEFAULT_RETENTION_DELETE_BATCH_SIZE);
        }

        var now = timeProvider.now();
        var doneCutoff = now.minus(Duration.ofDays(properties.getDoneRetentionDays()));
        var failedCutoff = now.minus(Duration.ofDays(properties.getFailedRetentionDays()));

        var totalDoneDeleted = 0;
        var totalFailedDeleted = 0;
        var batchesExecuted = 0;
        while (true) {
            var doneDeleted = eventQueueRepository.deleteEntriesByStatusOlderThan(
                    EventQueueEntryStatus.DONE, doneCutoff, batchSize);
            var failedDeleted = eventQueueRepository.deleteEntriesByStatusOlderThan(
                    EventQueueEntryStatus.FAILED, failedCutoff, batchSize);
            totalDoneDeleted += doneDeleted;
            totalFailedDeleted += failedDeleted;
            batchesExecuted++;

            if (doneDeleted < batchSize && failedDeleted < batchSize) {
                break;
            }
            try {
                Thread.sleep(DELETE_BATCH_PAUSE);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.warn("Event queue retention cleanup interrupted between delete batches. trigger={}", trigger);
                break;
            }
        }

        log.info(
                "Event queue retention cleanup finished. trigger={}, doneDeleted={}, failedDeleted={}, doneCutoff={}, failedCutoff={}, batchSize={}, batchesExecuted={}",
                trigger,
                totalDoneDeleted,
                totalFailedDeleted,
                doneCutoff,
                failedCutoff,
                batchSize,
                batchesExecuted);
        return new RetentionCleanupResult(totalDoneDeleted, totalFailedDeleted, batchesExecuted, true);
    }

    public record RetentionCleanupResult(int doneDeleted, int failedDeleted, int batchesExecuted, boolean executed) {}
}
