package app.partsvibe.shared.events.handling;

import app.partsvibe.shared.events.model.Event;

/**
 * Handles domain/integration events.
 *
 * <p>Implementations must be idempotent because the event queue provides at-least-once delivery.
 */
public interface EventHandler<E extends Event> {
    void handle(E event);
}
