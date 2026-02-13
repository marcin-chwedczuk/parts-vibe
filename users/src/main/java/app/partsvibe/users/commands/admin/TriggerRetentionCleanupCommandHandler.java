package app.partsvibe.users.commands.admin;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.events.admin.RetentionCleanupRequestedEvent;
import app.partsvibe.shared.events.publishing.EventPublisher;
import app.partsvibe.shared.request.RequestIdProvider;
import app.partsvibe.shared.time.TimeProvider;
import org.springframework.stereotype.Component;

@Component
class TriggerRetentionCleanupCommandHandler
        extends BaseCommandHandler<TriggerRetentionCleanupCommand, TriggerRetentionCleanupCommandResult> {
    private final EventPublisher eventPublisher;
    private final RequestIdProvider requestIdProvider;
    private final TimeProvider timeProvider;

    TriggerRetentionCleanupCommandHandler(
            EventPublisher eventPublisher, RequestIdProvider requestIdProvider, TimeProvider timeProvider) {
        this.eventPublisher = eventPublisher;
        this.requestIdProvider = requestIdProvider;
        this.timeProvider = timeProvider;
    }

    @Override
    protected TriggerRetentionCleanupCommandResult doHandle(TriggerRetentionCleanupCommand command) {
        var requestId = requestIdProvider.current().orElse(null);
        var event = RetentionCleanupRequestedEvent.create(requestId, timeProvider.now());
        eventPublisher.publish(event);
        return new TriggerRetentionCleanupCommandResult(event.eventId());
    }
}
