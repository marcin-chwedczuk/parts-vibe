package app.partsvibe.infra.events;

import app.partsvibe.shared.events.Event;
import app.partsvibe.shared.events.EventHandler;
import app.partsvibe.shared.events.EventTypeName;
import app.partsvibe.shared.events.EventTypeRegistry;
import app.partsvibe.shared.events.UnknownEventTypeException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;

@Component
public class SpringEventTypeRegistry implements EventTypeRegistry {
    private final Map<String, Class<? extends Event>> eventTypeToClass;
    private final Map<Class<? extends Event>, String> eventClassToType;

    public SpringEventTypeRegistry(List<EventHandler<?>> eventHandlers) {
        Map<String, Class<? extends Event>> byType = new LinkedHashMap<>();
        Map<Class<? extends Event>, String> byClass = new LinkedHashMap<>();
        for (EventHandler<?> handler : eventHandlers) {
            Class<? extends Event> eventClass = resolveHandledEventClass(handler);
            String eventType = resolveEventTypeName(eventClass);

            Class<? extends Event> existingClass = byType.get(eventType);
            if (existingClass != null && !existingClass.equals(eventClass)) {
                throw new IllegalStateException(
                        "Duplicate event type mapping to different classes. eventType=%s, classes=%s,%s"
                                .formatted(eventType, existingClass.getName(), eventClass.getName()));
            }

            String existingType = byClass.get(eventClass);
            if (existingType != null && !existingType.equals(eventType)) {
                throw new IllegalStateException(
                        "Duplicate event class mapping to different types. eventClass=%s, types=%s,%s"
                                .formatted(eventClass.getName(), existingType, eventType));
            }

            byType.put(eventType, eventClass);
            byClass.put(eventClass, eventType);
        }
        this.eventTypeToClass = Map.copyOf(byType);
        this.eventClassToType = Map.copyOf(byClass);
    }

    @Override
    public Class<? extends Event> eventClassFor(String eventType) {
        Class<? extends Event> eventClass = eventTypeToClass.get(eventType);
        if (eventClass == null) {
            throw new UnknownEventTypeException("Unknown event type: " + eventType);
        }
        return eventClass;
    }

    @Override
    public String eventTypeFor(Class<? extends Event> eventClass) {
        String eventType = eventClassToType.get(eventClass);
        if (eventType == null) {
            throw new UnknownEventTypeException("Unknown event class: " + eventClass.getName());
        }
        return eventType;
    }

    @Override
    public Collection<String> supportedEventTypes() {
        return eventTypeToClass.keySet();
    }

    @SuppressWarnings("unchecked")
    private static Class<? extends Event> resolveHandledEventClass(EventHandler<?> handler) {
        Class<?> userClass = ClassUtils.getUserClass(handler.getClass());
        ResolvableType type = ResolvableType.forClass(userClass).as(EventHandler.class);
        Class<?> resolved = type.getGeneric(0).resolve();
        if (resolved == null || !Event.class.isAssignableFrom(resolved)) {
            throw new IllegalStateException("Cannot resolve handled event class for handler: " + userClass.getName());
        }
        return (Class<? extends Event>) resolved;
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
