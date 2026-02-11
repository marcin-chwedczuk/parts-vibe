package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.events.serialization.EventJsonSerializer;
import app.partsvibe.shared.events.handling.EventHandler;
import app.partsvibe.shared.events.model.Event;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;

@Component
public class SpringEventDispatcher implements EventDispatcher {
    private static final Logger log = LoggerFactory.getLogger(SpringEventDispatcher.class);

    private final EventTypeRegistry eventTypeRegistry;
    private final EventJsonSerializer eventJsonSerializer;
    private final ListableBeanFactory beanFactory;
    private final Counter dispatchAttemptsCounter;
    private final Counter dispatchSuccessCounter;
    private final Counter dispatchErrorsCounter;

    public SpringEventDispatcher(
            EventTypeRegistry eventTypeRegistry,
            EventJsonSerializer eventJsonSerializer,
            ListableBeanFactory beanFactory,
            MeterRegistry meterRegistry) {
        this.eventTypeRegistry = eventTypeRegistry;
        this.eventJsonSerializer = eventJsonSerializer;
        this.beanFactory = beanFactory;
        this.dispatchAttemptsCounter = meterRegistry.counter("app.events.dispatch.attempts");
        this.dispatchSuccessCounter = meterRegistry.counter("app.events.dispatch.success");
        this.dispatchErrorsCounter = meterRegistry.counter("app.events.dispatch.errors");
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatch(String eventType, String payloadJson) {
        dispatchAttemptsCounter.increment();
        try {
            Class<? extends Event> eventClass = eventTypeRegistry.eventClassFor(eventType);
            log.debug(
                    "Resolved event class for dispatch. eventType={}, eventClass={}", eventType, eventClass.getName());
            Event event = eventJsonSerializer.deserialize(payloadJson, eventClass);
            List<ResolvedHandler> handlers = resolveHandlers(eventClass);
            log.debug(
                    "Dispatching event to handlers. eventType={}, eventClass={}, handlersCount={}",
                    eventType,
                    eventClass.getName(),
                    handlers.size());
            for (ResolvedHandler resolvedHandler : handlers) {
                String handlerClassName = handlerClassName(resolvedHandler.handler());
                log.debug(
                        "Invoking event handler. eventType={}, handlerBeanName={}, handlerClass={}",
                        eventType,
                        resolvedHandler.beanName(),
                        handlerClassName);
                try {
                    invokeHandler(resolvedHandler.handler(), event);
                } catch (RuntimeException ex) {
                    dispatchErrorsCounter.increment();
                    String causeMessage = safeMessage(ex);
                    log.error(
                            "Event handler execution failed. eventType={}, handlerBeanName={}, handlerClass={}, errorType={}, errorMessage={}",
                            eventType,
                            resolvedHandler.beanName(),
                            handlerClassName,
                            ex.getClass().getSimpleName(),
                            causeMessage,
                            ex);
                    throw new EventDispatchException(
                            "Event handler failed. eventType=%s, handlerBeanName=%s, handlerClass=%s, cause=%s: %s"
                                    .formatted(
                                            eventType,
                                            resolvedHandler.beanName(),
                                            handlerClassName,
                                            ex.getClass().getSimpleName(),
                                            causeMessage),
                            ex);
                }
            }
            dispatchSuccessCounter.increment();
            log.info("Dispatched event. eventType={}, handlersCount={}", eventType, handlers.size());
        } catch (EventDispatchException e) {
            throw e;
        } catch (RuntimeException e) {
            dispatchErrorsCounter.increment();
            String causeMessage = safeMessage(e);
            log.error(
                    "Event dispatch failed before handler execution. eventType={}, errorType={}, errorMessage={}",
                    eventType,
                    e.getClass().getSimpleName(),
                    causeMessage,
                    e);
            throw new EventDispatchException(
                    "Event dispatch failed. eventType=%s, cause=%s: %s"
                            .formatted(eventType, e.getClass().getSimpleName(), causeMessage),
                    e);
        }
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

    private static String safeMessage(Throwable throwable) {
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            return "<no-message>";
        }
        return message;
    }

    @SuppressWarnings("unchecked")
    private static <E extends Event> void invokeHandler(EventHandler<? extends Event> rawHandler, Event event) {
        EventHandler<E> typedHandler = (EventHandler<E>) rawHandler;
        typedHandler.handle((E) event);
    }

    private record ResolvedHandler(String beanName, EventHandler<? extends Event> handler) {}
}
