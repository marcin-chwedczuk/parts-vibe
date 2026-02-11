package app.partsvibe.shared.events.handling;

import app.partsvibe.shared.events.model.Event;

public interface EventHandler<E extends Event> {
    void handle(E event);
}
