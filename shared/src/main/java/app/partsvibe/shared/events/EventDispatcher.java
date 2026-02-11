package app.partsvibe.shared.events;

public interface EventDispatcher {
    void dispatch(String eventType, String payloadJson);
}
