package app.partsvibe.infra.events;

import app.partsvibe.shared.events.Event;
import app.partsvibe.shared.events.EventDispatchException;
import app.partsvibe.shared.events.EventDispatcher;
import app.partsvibe.shared.events.EventHandler;
import app.partsvibe.shared.events.EventJsonSerializer;
import app.partsvibe.shared.events.EventTypeRegistry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private final Map<Class<? extends Event>, List<EventHandler<? extends Event>>> handlersByEventClass;
    private final Counter dispatchAttemptsCounter;
    private final Counter dispatchSuccessCounter;
    private final Counter dispatchErrorsCounter;

    public SpringEventDispatcher(
            EventTypeRegistry eventTypeRegistry,
            EventJsonSerializer eventJsonSerializer,
            List<EventHandler<?>> handlers,
            MeterRegistry meterRegistry) {
        this.eventTypeRegistry = eventTypeRegistry;
        this.eventJsonSerializer = eventJsonSerializer;
        this.handlersByEventClass = buildHandlersMap(handlers);
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
            Event event = eventJsonSerializer.deserialize(payloadJson, eventClass);
            List<EventHandler<? extends Event>> handlers = handlersByEventClass.getOrDefault(eventClass, List.of());
            for (EventHandler<? extends Event> handler : handlers) {
                invokeHandler(handler, event);
            }
            dispatchSuccessCounter.increment();
            log.info("Dispatched event. eventType={}, handlersCount={}", eventType, handlers.size());
        } catch (RuntimeException e) {
            dispatchErrorsCounter.increment();
            throw new EventDispatchException("Failed to dispatch event. eventType=%s".formatted(eventType), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <E extends Event> void invokeHandler(EventHandler<? extends Event> rawHandler, Event event) {
        EventHandler<E> typedHandler = (EventHandler<E>) rawHandler;
        typedHandler.handle((E) event);
    }

    @SuppressWarnings("unchecked")
    private static Map<Class<? extends Event>, List<EventHandler<? extends Event>>> buildHandlersMap(
            List<EventHandler<?>> handlers) {
        Map<Class<? extends Event>, List<EventHandler<? extends Event>>> handlersMap = new LinkedHashMap<>();
        for (EventHandler<?> handler : handlers) {
            Class<?> userClass = ClassUtils.getUserClass(handler.getClass());
            ResolvableType type = ResolvableType.forClass(userClass).as(EventHandler.class);
            Class<?> resolved = type.getGeneric(0).resolve();
            if (resolved == null || !Event.class.isAssignableFrom(resolved)) {
                throw new IllegalStateException(
                        "Cannot resolve handled event class for handler: " + userClass.getName());
            }
            Class<? extends Event> eventClass = (Class<? extends Event>) resolved;
            handlersMap
                    .computeIfAbsent(eventClass, ignored -> new ArrayList<>())
                    .add((EventHandler<? extends Event>) handler);
        }
        return Map.copyOf(handlersMap);
    }
}
