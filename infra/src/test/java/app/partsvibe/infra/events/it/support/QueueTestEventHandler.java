package app.partsvibe.infra.events.it.support;

import app.partsvibe.shared.error.ApplicationException;
import app.partsvibe.shared.events.handling.BaseEventHandler;
import app.partsvibe.shared.events.handling.HandlesEvent;
import app.partsvibe.shared.events.model.EventMetadata;
import app.partsvibe.shared.request.RequestIdProvider;
import org.springframework.stereotype.Component;

@Component
@HandlesEvent(name = QueueTestEvent.EVENT_NAME, version = 1)
public class QueueTestEventHandler extends BaseEventHandler<QueueTestEvent> {
    private final QueueTestEventProbe probe;
    private final RequestIdProvider requestIdProvider;

    public QueueTestEventHandler(QueueTestEventProbe probe, RequestIdProvider requestIdProvider) {
        this.probe = probe;
        this.requestIdProvider = requestIdProvider;
    }

    @Override
    protected void doHandle(QueueTestEvent event, EventMetadata metadata) {
        int attempt = probe.incrementAttempts(event.key());
        probe.incrementActiveHandlers();
        try {
            if (event.processingDelayMs() > 0) {
                sleep(event.processingDelayMs());
            }
            if (attempt <= event.failAttempts()) {
                throw new ApplicationException("Synthetic handler failure for tests.");
            }
            probe.markCompleted(
                    event.key(), metadata, requestIdProvider.current().orElse(null));
        } finally {
            probe.decrementActiveHandlers();
        }
    }

    private static void sleep(long processingDelayMs) {
        try {
            Thread.sleep(processingDelayMs);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ApplicationException("Synthetic handler interrupted for tests.", ex);
        }
    }
}
