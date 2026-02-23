package app.partsvibe.shared.events.handling;

import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.EventMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public abstract class BaseEventHandler<E extends Event> implements EventHandler<E> {
    protected static final Logger log = LoggerFactory.getLogger(BaseEventHandler.class);

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void handle(E event) {
        handle(event, EventMetadata.fromEvent(event));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void handle(E event, EventMetadata metadata) {
        log.debug(
                "Event handler invoked. handlerClass={}, eventClass={}, eventId={}, eventName={}, schemaVersion={}",
                getClass().getSimpleName(),
                event.getClass().getSimpleName(),
                event.eventId(),
                metadata.eventName(),
                metadata.schemaVersion());
        doHandle(event, metadata);
    }

    protected void doHandle(E event, EventMetadata metadata) {
        doHandle(event);
    }

    protected void doHandle(E event) {
        throw new UnsupportedOperationException(
                "Implement doHandle(event, metadata) or override doHandle(event). handler=%s"
                        .formatted(getClass().getName()));
    }
}
