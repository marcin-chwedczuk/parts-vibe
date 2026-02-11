package app.partsvibe.infra.events.handling;

public interface EventDispatcher {
    void dispatch(String eventType, String payloadJson);
}
