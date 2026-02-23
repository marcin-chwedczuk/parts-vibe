package app.partsvibe.infra.events.it.support;

import app.partsvibe.shared.events.handling.BaseEventHandler;
import app.partsvibe.shared.events.handling.HandlesEvent;
import app.partsvibe.shared.events.model.EventMetadata;
import app.partsvibe.shared.request.RequestIdProvider;
import org.springframework.stereotype.Component;

@Component
@HandlesEvent(name = QueueTestEvent.EVENT_NAME, version = 1)
public class QueueTestEventSecondaryHandler extends BaseEventHandler<QueueTestEvent> {
    public static final String HANDLER_MARKER = "secondary";

    private final QueueTestEventProbe probe;
    private final RequestIdProvider requestIdProvider;

    public QueueTestEventSecondaryHandler(QueueTestEventProbe probe, RequestIdProvider requestIdProvider) {
        this.probe = probe;
        this.requestIdProvider = requestIdProvider;
    }

    @Override
    protected void doHandle(QueueTestEvent event, EventMetadata metadata) {
        probe.markCompleted(
                HANDLER_MARKER,
                event.key(),
                metadata,
                requestIdProvider.current().orElse(null));
    }
}
