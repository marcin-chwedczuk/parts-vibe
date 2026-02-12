package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.events.jpa.EventQueueEntryStatus;
import app.partsvibe.infra.events.jpa.EventQueueRepository;
import app.partsvibe.shared.time.TimeProvider;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EventQueueRetentionCleanupService {
    private static final Logger log = LoggerFactory.getLogger(EventQueueRetentionCleanupService.class);
    private static final long DELETE_BATCH_PAUSE_MS = 50L;
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

        int configuredBatchSize = properties.getRetentionDeleteBatchSize();
        int batchSize = configuredBatchSize > 0 ? configuredBatchSize : DEFAULT_RETENTION_DELETE_BATCH_SIZE;
        if (configuredBatchSize <= 0) {
            log.warn(
                    "Invalid retentionDeleteBatchSize configured ({}). Falling back to default batchSize={}.",
                    configuredBatchSize,
                    DEFAULT_RETENTION_DELETE_BATCH_SIZE);
        }

        Instant now = timeProvider.now();
        Instant doneCutoff = now.minusSeconds((long) properties.getDoneRetentionDays() * 24 * 60 * 60);
        Instant failedCutoff = now.minusSeconds((long) properties.getFailedRetentionDays() * 24 * 60 * 60);

        int totalDoneDeleted = 0;
        int totalFailedDeleted = 0;
        int batchesExecuted = 0;
        while (true) {
            int doneDeleted = eventQueueRepository.deleteEntriesByStatusOlderThan(
                    EventQueueEntryStatus.DONE, doneCutoff, batchSize);
            int failedDeleted = eventQueueRepository.deleteEntriesByStatusOlderThan(
                    EventQueueEntryStatus.FAILED, failedCutoff, batchSize);
            totalDoneDeleted += doneDeleted;
            totalFailedDeleted += failedDeleted;
            batchesExecuted++;

            if (doneDeleted < batchSize && failedDeleted < batchSize) {
                break;
            }
            try {
                Thread.sleep(DELETE_BATCH_PAUSE_MS);
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
