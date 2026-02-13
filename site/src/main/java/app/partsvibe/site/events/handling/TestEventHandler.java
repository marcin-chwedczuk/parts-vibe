package app.partsvibe.site.events.handling;

import app.partsvibe.shared.events.handling.BaseEventHandler;
import app.partsvibe.shared.events.handling.HandlesEvent;
import app.partsvibe.site.events.TestEvent;
import org.springframework.stereotype.Component;

@Component
@HandlesEvent(name = TestEvent.EVENT_NAME)
public class TestEventHandler extends BaseEventHandler<TestEvent> {
    @Override
    protected void doHandle(TestEvent event) {
        log.info("Handling TestEvent. eventId={}, message={}", event.eventId(), event.message());
        if (event.message() != null && event.message().contains("FAILED")) {
            throw new IllegalStateException("TestEvent handler forced failure due to FAILED marker.");
        }
    }
}
