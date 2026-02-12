package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.events.jpa.ClaimedEventQueueEntry;
import app.partsvibe.infra.events.serialization.EventJsonSerializer;
import app.partsvibe.shared.events.handling.EventHandler;
import app.partsvibe.shared.events.model.Event;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
public class EventQueueConsumer {
    private static final Logger log = LoggerFactory.getLogger(EventQueueConsumer.class);

    private final EventTypeRegistry eventTypeRegistry;
    private final EventJsonSerializer eventJsonSerializer;
    private final ListableBeanFactory beanFactory;

    public EventQueueConsumer(
            EventTypeRegistry eventTypeRegistry,
            EventJsonSerializer eventJsonSerializer,
            ListableBeanFactory beanFactory) {
        this.eventTypeRegistry = eventTypeRegistry;
        this.eventJsonSerializer = eventJsonSerializer;
        this.beanFactory = beanFactory;
    }

    public void handle(ClaimedEventQueueEntry entry) {
        Class<? extends Event> eventClass = eventTypeRegistry.eventClassFor(entry.eventType(), entry.schemaVersion());
        Event event = eventJsonSerializer.deserialize(entry.payload(), eventClass);
        List<ResolvedHandler> handlers = resolveHandlers(eventClass);

        log.debug(
                "Dispatching event queue entry to handlers. entry={}, handlersCount={}",
                entry.toStringWithoutPayload(),
                handlers.size());

        for (ResolvedHandler resolvedHandler : handlers) {
            String handlerClassName = handlerClassName(resolvedHandler.handler());
            log.debug(
                    "Invoking event handler. entry={}, handlerBeanName={}, handlerClass={}",
                    entry.toStringWithoutPayload(),
                    resolvedHandler.beanName(),
                    handlerClassName);
            try {
                invokeHandler(resolvedHandler.handler(), event);
            } catch (RuntimeException ex) {
                String causeMessage = safeMessage(ex);
                log.error(
                        "Event handler execution failed. entry={}, handlerBeanName={}, handlerClass={}, errorType={}, errorMessage={}",
                        entry.toStringWithoutPayload(),
                        resolvedHandler.beanName(),
                        handlerClassName,
                        ex.getClass().getSimpleName(),
                        causeMessage,
                        ex);
                throw new EventDispatchException(
                        "Event handler failed. eventId=%s, eventName=%s, schemaVersion=%d, handlerBeanName=%s, handlerClass=%s, cause=%s: %s"
                                .formatted(
                                        entry.eventId(),
                                        entry.eventType(),
                                        entry.schemaVersion(),
                                        resolvedHandler.beanName(),
                                        handlerClassName,
                                        ex.getClass().getSimpleName(),
                                        causeMessage),
                        ex);
            }

            if (Thread.currentThread().isInterrupted()) {
                throw new EventDispatchException(
                        "Event handling thread interrupted. eventId=%s, eventName=%s, schemaVersion=%d"
                                .formatted(entry.eventId(), entry.eventType(), entry.schemaVersion()));
            }
        }
    }

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return "<no-message>";
        }
        return message;
    }

    private static String handlerClassName(EventHandler<? extends Event> handler) {
        return ClassUtils.getUserClass(handler.getClass()).getName();
    }

    @SuppressWarnings("unchecked")
    private List<ResolvedHandler> resolveHandlers(Class<? extends Event> eventClass) {
        ResolvableType handlerType = ResolvableType.forClassWithGenerics(EventHandler.class, eventClass);
        String[] beanNames = beanFactory.getBeanNamesForType(EventHandler.class, true, false);
        List<ResolvedHandler> handlers = new ArrayList<>();
        for (String beanName : beanNames) {
            if (!beanFactory.isTypeMatch(beanName, handlerType)) {
                continue;
            }
            EventHandler<? extends Event> handler = (EventHandler<? extends Event>) beanFactory.getBean(beanName);
            handlers.add(new ResolvedHandler(beanName, handler));
        }
        return handlers;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Event> void invokeHandler(EventHandler<? extends Event> rawHandler, Event event) {
        EventHandler<E> typedHandler = (EventHandler<E>) rawHandler;
        typedHandler.handle((E) event);
    }

    private record ResolvedHandler(String beanName, EventHandler<? extends Event> handler) {}
}
