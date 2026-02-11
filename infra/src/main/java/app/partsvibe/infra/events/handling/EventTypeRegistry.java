package app.partsvibe.infra.events.handling;

import app.partsvibe.shared.events.model.Event;
import java.util.Collection;

public interface EventTypeRegistry {
    Class<? extends Event> eventClassFor(String eventType);

    String eventTypeFor(Class<? extends Event> eventClass);

    Collection<String> supportedEventTypes();
}
