package app.partsvibe.shared.events.publishing;

import app.partsvibe.shared.events.model.Event;

public interface EventPublisher {
    void publish(Event event);
}
