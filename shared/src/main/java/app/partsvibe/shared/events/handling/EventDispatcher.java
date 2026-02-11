package app.partsvibe.shared.events.handling;

public interface EventDispatcher {
    void dispatch(String eventType, String payloadJson);
}
