package app.partsvibe.users.commands.admin;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.events.admin.RetentionCleanupRequestedEvent;
import app.partsvibe.shared.events.publishing.EventPublisher;
import org.springframework.stereotype.Component;

@Component
class TriggerRetentionCleanupCommandHandler
        extends BaseCommandHandler<TriggerRetentionCleanupCommand, TriggerRetentionCleanupCommandResult> {
    private final EventPublisher eventPublisher;

    TriggerRetentionCleanupCommandHandler(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected TriggerRetentionCleanupCommandResult doHandle(TriggerRetentionCleanupCommand command) {
        var event = RetentionCleanupRequestedEvent.create();
        eventPublisher.publish(event);
        return new TriggerRetentionCleanupCommandResult(event.eventId());
    }
}
