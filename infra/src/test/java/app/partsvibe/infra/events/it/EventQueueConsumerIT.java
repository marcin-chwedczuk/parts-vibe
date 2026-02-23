package app.partsvibe.infra.events.it;

import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.infra.events.handling.EventQueueConsumer;
import app.partsvibe.infra.events.it.support.QueueTestEvent;
import app.partsvibe.infra.events.it.support.QueueTestEventProbe;
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
        QueueTestEvent event = QueueTestEvent.create("consumer", 0, 0);
        var entry = new ClaimedEventQueueEntry(
                100L,
                event.eventId(),
                QueueTestEvent.EVENT_NAME,
                1,
                Instant.now(),
                eventJsonSerializer.serialize(event),
                "req-consumer-1",
                "bob@example.com",
                1);

        eventQueueConsumer.handle(entry);

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(25))
                .untilAsserted(() -> assertThat(probe.completions("consumer")).isEqualTo(1));

        var metadata = probe.lastMetadata("consumer");
        assertThat(metadata).isNotNull();
        assertThat(metadata.eventId()).isEqualTo(event.eventId());
        assertThat(metadata.eventName()).isEqualTo(QueueTestEvent.EVENT_NAME);
        assertThat(metadata.schemaVersion()).isEqualTo(1);
        assertThat(metadata.requestId()).isEqualTo("req-consumer-1");
        assertThat(metadata.publishedBy()).isEqualTo("bob@example.com");
        assertThat(metadata.publishedAt()).isNotNull();
        assertThat(probe.lastRequestIdInScope("consumer")).isEqualTo("req-consumer-1");
    }
}
