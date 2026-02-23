package app.partsvibe.infra.events.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.infra.events.handling.EventDispatchException;
import app.partsvibe.infra.events.handling.EventQueueConsumer;
import app.partsvibe.infra.events.it.support.QueueAlwaysFailEvent;
import app.partsvibe.infra.events.it.support.QueueTestEvent;
import app.partsvibe.infra.events.it.support.QueueTestEventHandler;
import app.partsvibe.infra.events.it.support.QueueTestEventProbe;
import app.partsvibe.infra.events.it.support.QueueTestEventSecondaryHandler;
import app.partsvibe.infra.events.jpa.ClaimedEventQueueEntry;
import app.partsvibe.infra.events.serialization.EventJsonSerializer;
import app.partsvibe.testsupport.fakes.InMemoryRequestIdProvider;
import java.time.Duration;
import java.time.Instant;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = EventQueueConsumerItTestApplication.class)
class EventQueueConsumerIT {
    private static final String CONSUMER_KEY = "consumer";
    private static final String CONSUMER_REQUEST_ID = "req-consumer-1";
    private static final String CONSUMER_PUBLISHED_BY = "bob@example.com";
    private static final String MULTI_HANDLERS_KEY = "multi-handlers";
    private static final String FAILING_HANDLER_KEY = "failing-handler";

    @Autowired
    private EventQueueConsumer eventQueueConsumer;

    @Autowired
    private EventJsonSerializer eventJsonSerializer;

    @Autowired
    private QueueTestEventProbe probe;

    @Autowired
    private InMemoryRequestIdProvider requestIdProvider;

    @BeforeEach
    void setUp() {
        probe.reset();
        requestIdProvider.clear();
    }

    @Test
    void consumerInvokesHandlerAndPropagatesMetadataAndRequestScope() {
        QueueTestEvent event = QueueTestEvent.create(CONSUMER_KEY, 0, 0);
        var entry = new ClaimedEventQueueEntry(
                100L,
                event.eventId(),
                QueueTestEvent.EVENT_NAME,
                1,
                Instant.now(),
                eventJsonSerializer.serialize(event),
                CONSUMER_REQUEST_ID,
                CONSUMER_PUBLISHED_BY,
                1);

        eventQueueConsumer.handle(entry);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(25))
                .untilAsserted(() -> assertThat(probe.completions(CONSUMER_KEY)).isEqualTo(2));

        var metadata = probe.lastMetadata(QueueTestEventHandler.HANDLER_MARKER, CONSUMER_KEY);
        assertThat(metadata).isNotNull();
        assertThat(metadata.eventId()).isEqualTo(event.eventId());
        assertThat(metadata.eventName()).isEqualTo(QueueTestEvent.EVENT_NAME);
        assertThat(metadata.schemaVersion()).isEqualTo(1);
        assertThat(metadata.requestId()).isEqualTo(CONSUMER_REQUEST_ID);
        assertThat(metadata.publishedBy()).isEqualTo(CONSUMER_PUBLISHED_BY);
        assertThat(metadata.publishedAt()).isNotNull();
        assertThat(probe.lastRequestIdInScope(QueueTestEventHandler.HANDLER_MARKER, CONSUMER_KEY))
                .isEqualTo(CONSUMER_REQUEST_ID);
        assertThat(probe.completions(QueueTestEventSecondaryHandler.HANDLER_MARKER, CONSUMER_KEY))
                .isEqualTo(1);
        assertThat(probe.lastRequestIdInScope(QueueTestEventSecondaryHandler.HANDLER_MARKER, CONSUMER_KEY))
                .isEqualTo(CONSUMER_REQUEST_ID);
    }

    @Test
    void consumerInvokesAllHandlersForSameEventSchema() {
        QueueTestEvent event = QueueTestEvent.create(MULTI_HANDLERS_KEY, 0, 0);
        var entry = new ClaimedEventQueueEntry(
                101L,
                event.eventId(),
                QueueTestEvent.EVENT_NAME,
                1,
                Instant.now(),
                eventJsonSerializer.serialize(event),
                "req-consumer-2",
                CONSUMER_PUBLISHED_BY,
                1);

        eventQueueConsumer.handle(entry);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(25))
                .untilAsserted(() -> {
                    assertThat(probe.completions(QueueTestEventHandler.HANDLER_MARKER, MULTI_HANDLERS_KEY))
                            .isEqualTo(1);
                    assertThat(probe.completions(QueueTestEventSecondaryHandler.HANDLER_MARKER, MULTI_HANDLERS_KEY))
                            .isEqualTo(1);
                });
    }

    @Test
    void consumerWrapsHandlerFailureInEventDispatchException() {
        QueueAlwaysFailEvent event = QueueAlwaysFailEvent.create(FAILING_HANDLER_KEY);
        var entry = new ClaimedEventQueueEntry(
                102L,
                event.eventId(),
                QueueAlwaysFailEvent.EVENT_NAME,
                1,
                Instant.now(),
                eventJsonSerializer.serialize(event),
                "req-consumer-3",
                CONSUMER_PUBLISHED_BY,
                1);

        assertThatThrownBy(() -> eventQueueConsumer.handle(entry))
                .isInstanceOf(EventDispatchException.class)
                .hasMessageContaining("handler failed")
                .hasMessageContaining(QueueAlwaysFailEvent.EVENT_NAME);
    }
}
