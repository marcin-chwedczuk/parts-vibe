package app.partsvibe.infra.events.admin;

import app.partsvibe.infra.events.handling.EventQueueRetentionCleanupService;
import app.partsvibe.shared.events.admin.TriggerRetentionCleanupEvent;
import app.partsvibe.shared.events.handling.BaseEventHandler;
import org.springframework.stereotype.Component;

@Component
public class TriggerRetentionCleanupEventHandler extends BaseEventHandler<TriggerRetentionCleanupEvent> {
    private final EventQueueRetentionCleanupService retentionCleanupService;

    public TriggerRetentionCleanupEventHandler(EventQueueRetentionCleanupService retentionCleanupService) {
        this.retentionCleanupService = retentionCleanupService;
    }

    @Override
    protected void doHandle(TriggerRetentionCleanupEvent event) {
        log.info(
                "Handling trigger retention cleanup event. eventId={}, requestId={}",
                event.eventId(),
                event.requestId().orElse("<none>"));
        retentionCleanupService.cleanup("admin-event");
    }
}
