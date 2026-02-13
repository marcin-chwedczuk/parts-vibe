package app.partsvibe.infra.events.handling;

import app.partsvibe.shared.events.handling.EventHandler;
import app.partsvibe.shared.events.handling.HandlesEvent;
import app.partsvibe.shared.events.model.Event;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
public class SpringEventTypeRegistry implements EventTypeRegistry {
    private static final Logger log = LoggerFactory.getLogger(SpringEventTypeRegistry.class);

    private final Map<EventTypeKey, List<ResolvedEventHandlerDescriptor>> handlersByType;

    public SpringEventTypeRegistry(ListableBeanFactory beanFactory) {
        Map<EventTypeKey, List<ResolvedEventHandlerDescriptor>> byType = new LinkedHashMap<>();
        String[] beanNames = beanFactory.getBeanNamesForType(EventHandler.class, true, false);

        for (String beanName : beanNames) {
            Class<?> beanType = beanFactory.getType(beanName);
            if (beanType == null) {
                throw new IllegalStateException("Unable to resolve event handler bean type. beanName=" + beanName);
            }

            Class<?> userClass = ClassUtils.getUserClass(beanType);
            HandlesEvent[] handles = userClass.getAnnotationsByType(HandlesEvent.class);
            if (handles.length == 0) {
                throw new IllegalStateException(
                        "Event handler must declare at least one @HandlesEvent mapping. handlerClass="
                                + userClass.getName());
            }

            Class<? extends Event> payloadClass = resolvePayloadClass(userClass, beanName);
            for (HandlesEvent handle : handles) {
                validateHandleAnnotation(handle, userClass);
                EventTypeKey key = new EventTypeKey(handle.name(), handle.version());
                byType.computeIfAbsent(key, ignored -> new ArrayList<>())
                        .add(new ResolvedEventHandlerDescriptor(
                                handle.name(), handle.version(), beanName, payloadClass));
            }
        }

        this.handlersByType = byType.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
        logDiscoveredHandlers(this.handlersByType);
    }

    @Override
    public List<ResolvedEventHandlerDescriptor> handlersFor(String eventType, int schemaVersion) {
        List<ResolvedEventHandlerDescriptor> handlers = handlersByType.get(new EventTypeKey(eventType, schemaVersion));
        if (handlers == null || handlers.isEmpty()) {
            throw new UnknownEventTypeException(
                    "No event handlers found for event type: %s, schemaVersion=%d".formatted(eventType, schemaVersion));
        }
        return handlers;
    }

    private static void validateHandleAnnotation(HandlesEvent handle, Class<?> userClass) {
        if (handle.name().isBlank()) {
            throw new IllegalStateException(
                    "@HandlesEvent name must be non-blank. handlerClass=%s".formatted(userClass.getName()));
        }
        if (handle.version() <= 0) {
            throw new IllegalStateException("@HandlesEvent version must be greater than 0. handlerClass=%s, version=%d"
                    .formatted(userClass.getName(), handle.version()));
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Event> resolvePayloadClass(Class<?> userClass, String beanName) {
        ResolvableType handlerType = ResolvableType.forClass(userClass).as(EventHandler.class);
        ResolvableType payloadType = handlerType.getGeneric(0);
        Class<?> resolved = payloadType.resolve();
        if (resolved == null) {
            throw new IllegalStateException(
                    "Cannot resolve EventHandler payload generic type. beanName=%s, handlerClass=%s"
                            .formatted(beanName, userClass.getName()));
        }
        if (!Event.class.isAssignableFrom(resolved)) {
            throw new IllegalStateException(
                    "EventHandler payload type must implement Event. beanName=%s, handlerClass=%s, payloadClass=%s"
                            .formatted(beanName, userClass.getName(), resolved.getName()));
        }
        return (Class<? extends Event>) resolved;
    }

    private static void logDiscoveredHandlers(Map<EventTypeKey, List<ResolvedEventHandlerDescriptor>> handlersByType) {
        if (handlersByType.isEmpty()) {
            log.info("Discovered event handlers: none");
            return;
        }
        String discovered = handlersByType.entrySet().stream()
                .sorted(Comparator.comparing((Map.Entry<EventTypeKey, ?> entry) ->
                                entry.getKey().eventType())
                        .thenComparingInt(entry -> entry.getKey().schemaVersion()))
                .map(entry -> "%s@v%d=[%s]"
                        .formatted(
                                entry.getKey().eventType(),
                                entry.getKey().schemaVersion(),
                                entry.getValue().stream()
                                        .map(descriptor -> "%s:%s"
                                                .formatted(
                                                        descriptor.beanName(),
                                                        descriptor
                                                                .payloadClass()
                                                                .getName()))
                                        .sorted()
                                        .collect(Collectors.joining(", "))))
                .collect(Collectors.joining(", "));
        log.info("Discovered event handlers. routes={}, mappings=[{}]", handlersByType.size(), discovered);
    }

    private record EventTypeKey(String eventType, int schemaVersion) {}
}
