package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.spring.ClasspathScanner;
import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.EventTypeName;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SpringEventTypeRegistry implements EventTypeRegistry {
    private static final Logger log = LoggerFactory.getLogger(SpringEventTypeRegistry.class);

    private final Map<String, Class<? extends Event>> eventTypeToClass;

    public SpringEventTypeRegistry(ClasspathScanner annotationClassScanner) {
        Map<String, Class<? extends Event>> byType = new LinkedHashMap<>();
        for (Class<?> discoveredClass : annotationClassScanner.findAnnotatedClasses(EventTypeName.class)) {
            if (!Event.class.isAssignableFrom(discoveredClass)) {
                throw new IllegalStateException(
                        "@EventTypeName class must implement Event: " + discoveredClass.getName());
            }
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) discoveredClass;
            String eventType = resolveEventTypeName(eventClass);

            Class<? extends Event> existingClass = byType.get(eventType);
            if (existingClass != null && !existingClass.equals(eventClass)) {
                throw new IllegalStateException(
                        "Duplicate event type mapping to different classes. eventType=%s, classes=%s,%s"
                                .formatted(eventType, existingClass.getName(), eventClass.getName()));
            }

            byType.put(eventType, eventClass);
        }
        this.eventTypeToClass = Map.copyOf(byType);
        logDiscoveredTypes(this.eventTypeToClass);
    }

    @Override
    public Class<? extends Event> eventClassFor(String eventType) {
        Class<? extends Event> eventClass = eventTypeToClass.get(eventType);
        if (eventClass == null) {
            throw new UnknownEventTypeException("Unknown event type: " + eventType);
        }
        return eventClass;
    }

    private static void logDiscoveredTypes(Map<String, Class<? extends Event>> eventTypes) {
        if (eventTypes.isEmpty()) {
            log.info("Discovered event types: none");
            return;
        }
        String discovered = eventTypes.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue().getName())
                .sorted()
                .collect(Collectors.joining(", "));
        log.info("Discovered event types. count={}, types=[{}]", eventTypes.size(), discovered);
    }

    private static String resolveEventTypeName(Class<? extends Event> eventClass) {
        EventTypeName annotation = eventClass.getAnnotation(EventTypeName.class);
        if (annotation == null || annotation.value().isBlank()) {
            throw new IllegalStateException(
                    "Event class must be annotated with @EventTypeName and non-blank value: " + eventClass.getName());
        }
        return annotation.value();
    }
}
