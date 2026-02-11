package app.partsvibe.shared.events;

public interface EventJsonSerializer {
    String serialize(Event event);

    <E extends Event> E deserialize(String payloadJson, Class<E> eventClass);
}
