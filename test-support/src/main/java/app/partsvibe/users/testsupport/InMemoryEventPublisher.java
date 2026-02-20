package app.partsvibe.users.testsupport;

import app.partsvibe.shared.events.model.Event;
import app.partsvibe.shared.events.publishing.EventPublisher;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryEventPublisher implements EventPublisher {
    private final List<Event> publishedEvents = new CopyOnWriteArrayList<>();

    @Override
    public void publish(Event event) {
        publishedEvents.add(event);
    }

    public List<Event> publishedEvents() {
        return List.copyOf(publishedEvents);
    }

    public void clear() {
        publishedEvents.clear();
    }
}
