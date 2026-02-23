package app.partsvibe.infra.events.it.support;

import app.partsvibe.shared.error.ApplicationException;
import app.partsvibe.shared.events.handling.BaseEventHandler;
import app.partsvibe.shared.events.handling.HandlesEvent;
import app.partsvibe.shared.events.model.EventMetadata;
import org.springframework.stereotype.Component;

@Component
@HandlesEvent(name = QueueAlwaysFailEvent.EVENT_NAME, version = 1)
public class QueueAlwaysFailEventHandler extends BaseEventHandler<QueueAlwaysFailEvent> {
    @Override
    protected void doHandle(QueueAlwaysFailEvent event, EventMetadata metadata) {
        throw new ApplicationException("Synthetic always-failing handler for tests.");
    }
}
