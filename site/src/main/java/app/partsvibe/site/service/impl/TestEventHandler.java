package app.partsvibe.site.service.impl;

import app.partsvibe.shared.events.EventHandler;
import app.partsvibe.site.events.TestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TestEventHandler implements EventHandler<TestEvent> {
    private static final Logger log = LoggerFactory.getLogger(TestEventHandler.class);

    @Override
    public void handle(TestEvent event) {
        log.info("Handling TestEvent. eventId={}, message={}", event.eventId(), event.message());
        if (event.message() != null && event.message().contains("FAILED")) {
            throw new IllegalStateException("TestEvent handler forced failure due to FAILED marker.");
        }
    }
}
