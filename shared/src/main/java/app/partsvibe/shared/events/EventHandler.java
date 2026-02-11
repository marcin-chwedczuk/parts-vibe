package app.partsvibe.shared.events;

public interface EventHandler<E extends Event> {
    void handle(E event);
}
