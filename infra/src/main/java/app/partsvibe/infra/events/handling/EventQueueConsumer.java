package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.events.jpa.ClaimedEventQueueEntry;
import app.partsvibe.infra.events.serialization.EventJsonSerializer;
import app.partsvibe.infra.utils.ThrowableUtils;
import app.partsvibe.shared.events.handling.EventHandler;
import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.request.RequestIdProvider;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;

@Component
public class EventQueueConsumer {
    private static final Logger log = LoggerFactory.getLogger(EventQueueConsumer.class);

    private final EventTypeRegistry eventTypeRegistry;
    private final EventJsonSerializer eventJsonSerializer;
    private final ListableBeanFactory beanFactory;
    private final RequestIdProvider requestIdProvider;

    public EventQueueConsumer(
            EventTypeRegistry eventTypeRegistry,
            EventJsonSerializer eventJsonSerializer,
            ListableBeanFactory beanFactory,
            RequestIdProvider requestIdProvider) {
        this.eventTypeRegistry = eventTypeRegistry;
        this.eventJsonSerializer = eventJsonSerializer;
        this.beanFactory = beanFactory;
        this.requestIdProvider = requestIdProvider;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handle(ClaimedEventQueueEntry entry) {
        List<ResolvedEventHandlerDescriptor> handlers =
                eventTypeRegistry.handlersFor(entry.eventType(), entry.schemaVersion());
        log.debug(
                "Dispatching event queue entry to handlers. entry={}, handlersCount={}",
                entry.toStringWithoutPayload(),
                handlers.size());

        for (ResolvedEventHandlerDescriptor descriptor : handlers) {
            // Deserialize per handler to isolate against mutable payload side effects.
            Event payload = deserialize(entry, descriptor.payloadClass());
            String requestId = payload.requestId()
                    .filter(value -> !value.isBlank())
                    .orElseGet(() -> UUID.randomUUID().toString());
            EventHandler<? extends Event> handler = handlerByBeanName(descriptor.beanName());
            String handlerClassName = handlerClassName(handler);
            log.debug(
                    "Invoking event handler. entry={}, handlerBeanName={}, handlerClass={}, payloadClass={}",
                    entry.toStringWithoutPayload(),
                    descriptor.beanName(),
                    handlerClassName,
                    descriptor.payloadClass().getName());
            try (var ignored = requestIdProvider.withRequestId(requestId)) {
                invokeHandler(handler, payload);
            } catch (RuntimeException ex) {
                String causeMessage = ThrowableUtils.safeMessage(ex);
                log.error(
                        "Event handler execution failed. entry={}, handlerBeanName={}, handlerClass={}, errorType={}, errorMessage={}",
                        entry.toStringWithoutPayload(),
                        descriptor.beanName(),
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
                                        descriptor.beanName(),
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

    private Event deserialize(ClaimedEventQueueEntry entry, Class<? extends Event> payloadClass) {
        return eventJsonSerializer.deserialize(entry.payload(), payloadClass);
    }

    @SuppressWarnings("unchecked")
    private EventHandler<? extends Event> handlerByBeanName(String beanName) {
        return (EventHandler<? extends Event>) beanFactory.getBean(beanName, EventHandler.class);
    }

    private static String handlerClassName(EventHandler<? extends Event> handler) {
        return ClassUtils.getUserClass(handler.getClass()).getName();
    }

    @SuppressWarnings("unchecked")
    private static <E extends Event> void invokeHandler(EventHandler<? extends Event> rawHandler, Event event) {
        EventHandler<E> typedHandler = (EventHandler<E>) rawHandler;
        typedHandler.handle((E) event);
    }
}
