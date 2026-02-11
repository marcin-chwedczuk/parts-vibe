package app.partsvibe.infra.events.serialization;

import app.partsvibe.shared.events.model.Event;

public interface EventJsonSerializer {
    String serialize(Event event);

    <E extends Event> E deserialize(String payloadJson, Class<E> eventClass);
}
