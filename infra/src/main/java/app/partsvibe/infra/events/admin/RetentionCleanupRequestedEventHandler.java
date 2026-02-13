package app.partsvibe.infra.events.admin;

import app.partsvibe.infra.events.handling.EventQueueRetentionCleanupService;
import app.partsvibe.shared.events.admin.RetentionCleanupRequestedEvent;
import app.partsvibe.shared.events.handling.BaseEventHandler;
import app.partsvibe.shared.events.handling.HandlesEvent;
import org.springframework.stereotype.Component;

@Component
@HandlesEvent(name = RetentionCleanupRequestedEvent.EVENT_NAME)
public class RetentionCleanupRequestedEventHandler extends BaseEventHandler<RetentionCleanupRequestedEvent> {
    private final EventQueueRetentionCleanupService retentionCleanupService;

    public RetentionCleanupRequestedEventHandler(EventQueueRetentionCleanupService retentionCleanupService) {
        this.retentionCleanupService = retentionCleanupService;
    }

    @Override
    protected void doHandle(RetentionCleanupRequestedEvent event) {
        log.info(
                "Handling retention cleanup requested event. eventId={}, requestId={}",
                event.eventId(),
                event.requestId().orElse("<none>"));
        retentionCleanupService.cleanup("admin-event");
    }
}
