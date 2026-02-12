package app.partsvibe.infra.events.handling;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EventQueueRetentionCleanupJob {
    private final EventQueueRetentionCleanupService retentionCleanupService;

    public EventQueueRetentionCleanupJob(EventQueueRetentionCleanupService retentionCleanupService) {
        this.retentionCleanupService = retentionCleanupService;
    }

    @Scheduled(fixedDelayString = "${app.events.dispatcher.retention-cleanup-interval-ms:3600000}")
    public void cleanup() {
        retentionCleanupService.cleanup("scheduled-job");
    }
}
