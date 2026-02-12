package app.partsvibe.infra.events.handling;

import app.partsvibe.infra.spring.ClasspathScanner;
import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.model.EventMetadata;
import app.partsvibe.shared.events.model.PublishableEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SpringEventTypeRegistry implements EventTypeRegistry {
    private static final Logger log = LoggerFactory.getLogger(SpringEventTypeRegistry.class);

    private final Map<EventTypeKey, Class<? extends Event>> eventTypeToClass;

    public SpringEventTypeRegistry(ClasspathScanner classpathScanner) {
        Map<EventTypeKey, Class<? extends Event>> byType = new LinkedHashMap<>();
        for (Class<?> discoveredClass : classpathScanner.findAnnotatedClasses(PublishableEvent.class)) {
            if (!Event.class.isAssignableFrom(discoveredClass)) {
                throw new IllegalStateException(
                        "@EventDescriptor class must implement Event: " + discoveredClass.getName());
            }
            @SuppressWarnings("unchecked")
            Class<? extends Event> eventClass = (Class<? extends Event>) discoveredClass;
            EventMetadata metadata = EventMetadata.fromEventClass(eventClass);
            EventTypeKey eventTypeKey = new EventTypeKey(metadata.eventName(), metadata.schemaVersion());

            Class<? extends Event> existingClass = byType.get(eventTypeKey);
            if (existingClass != null && !existingClass.equals(eventClass)) {
                throw new IllegalStateException(
                        "Duplicate event type mapping to different classes. eventName=%s, schemaVersion=%d, classes=%s,%s"
                                .formatted(
                                        eventTypeKey.eventType(),
                                        eventTypeKey.schemaVersion(),
                                        existingClass.getName(),
                                        eventClass.getName()));
            }

            byType.put(eventTypeKey, eventClass);
        }
        this.eventTypeToClass = Map.copyOf(byType);
        logDiscoveredTypes(this.eventTypeToClass);
    }

    @Override
    public Class<? extends Event> eventClassFor(String eventType, int schemaVersion) {
        Class<? extends Event> eventClass = eventTypeToClass.get(new EventTypeKey(eventType, schemaVersion));
        if (eventClass == null) {
            throw new UnknownEventTypeException(
                    "Unknown event type: %s, schemaVersion=%d".formatted(eventType, schemaVersion));
        }
        return eventClass;
    }

    private static void logDiscoveredTypes(Map<EventTypeKey, Class<? extends Event>> eventTypes) {
        if (eventTypes.isEmpty()) {
            log.info("Discovered event types: none");
            return;
        }
        String discovered = eventTypes.entrySet().stream()
                .map(entry -> "%s@v%d=%s"
                        .formatted(
                                entry.getKey().eventType(),
                                entry.getKey().schemaVersion(),
                                entry.getValue().getName()))
                .sorted()
                .collect(Collectors.joining(", "));
        log.info("Discovered event types. count={}, types=[{}]", eventTypes.size(), discovered);
    }

    private record EventTypeKey(String eventType, int schemaVersion) {}
}
