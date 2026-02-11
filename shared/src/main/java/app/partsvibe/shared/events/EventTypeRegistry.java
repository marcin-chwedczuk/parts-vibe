package app.partsvibe.shared.events;

import java.util.Collection;

public interface EventTypeRegistry {
    Class<? extends Event> eventClassFor(String eventType);

    String eventTypeFor(Class<? extends Event> eventClass);

    Collection<String> supportedEventTypes();
}
