package app.partsvibe.shared.events.handling;

import app.partsvibe.shared.events.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public abstract class BaseEventHandler<E extends Event> implements EventHandler<E> {
    protected static final Logger log = LoggerFactory.getLogger(BaseEventHandler.class);

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void handle(E event) {
        log.debug(
                "Event handler invoked. handlerClass={}, eventClass={}, eventId={}",
                getClass().getSimpleName(),
                event.getClass().getSimpleName(),
                event.eventId());
        doHandle(event);
    }

    protected abstract void doHandle(E event);
}
