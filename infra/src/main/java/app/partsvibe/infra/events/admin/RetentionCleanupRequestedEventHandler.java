package app.partsvibe.infra.events.admin;

import app.partsvibe.infra.events.handling.EventQueueRetentionCleanupService;
import app.partsvibe.shared.events.admin.RetentionCleanupRequestedEvent;
import app.partsvibe.shared.events.handling.BaseEventHandler;
import app.partsvibe.shared.events.handling.HandlesEvent;
import app.partsvibe.shared.events.model.EventMetadata;
import org.springframework.stereotype.Component;

@Component
@HandlesEvent(name = RetentionCleanupRequestedEvent.EVENT_NAME)
public class RetentionCleanupRequestedEventHandler extends BaseEventHandler<RetentionCleanupRequestedEvent> {
    private final EventQueueRetentionCleanupService retentionCleanupService;

    public RetentionCleanupRequestedEventHandler(EventQueueRetentionCleanupService retentionCleanupService) {
        this.retentionCleanupService = retentionCleanupService;
    }

    @Override
    protected void doHandle(RetentionCleanupRequestedEvent event, EventMetadata metadata) {
        log.info("Handling retention cleanup requested event. eventId={}, metadata={}", event.eventId(), metadata);
        retentionCleanupService.cleanup("admin-event");
    }
}
