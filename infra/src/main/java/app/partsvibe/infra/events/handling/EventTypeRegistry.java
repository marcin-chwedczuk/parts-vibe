package app.partsvibe.infra.events.handling;

import app.partsvibe.shared.events.model.Event;

public interface EventTypeRegistry {
    Class<? extends Event> eventClassFor(String eventType, int schemaVersion);
}
