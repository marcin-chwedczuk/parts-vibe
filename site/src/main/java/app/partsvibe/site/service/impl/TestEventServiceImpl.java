package app.partsvibe.site.service.impl;

import app.partsvibe.shared.events.publishing.EventPublisher;
import app.partsvibe.site.events.TestEvent;
import app.partsvibe.site.service.TestEventService;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TestEventServiceImpl implements TestEventService {
    private final EventPublisher eventPublisher;

    public TestEventServiceImpl(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public UUID publishTestEvent(String requestId, String message) {
        TestEvent event = TestEvent.create(requestId, message);
        eventPublisher.publish(event);
        return event.eventId();
    }
}
