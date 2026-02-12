package app.partsvibe.site.commands.testevent;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.events.publishing.EventPublisher;
import app.partsvibe.site.events.TestEvent;
import org.springframework.stereotype.Component;

@Component
class PublishTestEventCommandHandler
        extends BaseCommandHandler<PublishTestEventCommand, PublishTestEventCommandResult> {
    private final EventPublisher eventPublisher;

    PublishTestEventCommandHandler(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    protected PublishTestEventCommandResult doHandle(PublishTestEventCommand command) {
        var event = TestEvent.create(command.requestId(), command.message());
        eventPublisher.publish(event);
        return new PublishTestEventCommandResult(event.eventId());
    }
}
