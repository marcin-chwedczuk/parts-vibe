package app.partsvibe.infra.events.it;

import static app.partsvibe.testsupport.assertions.JsonAssertions.assertJsonEquals;
import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.infra.events.it.support.QueueTestEvent;
import app.partsvibe.infra.events.jpa.EventQueueEntry;
import app.partsvibe.infra.events.jpa.EventQueueEntryStatus;
import org.junit.jupiter.api.Test;

class EventQueuePublisherIT extends AbstractEventQueueDatabaseIntegrationTest {
    @Test
    void publisherStoresQueueEntryWithInfrastructureMetadata() {
        requestIdProvider.set("req-publisher-1");
        currentUserProvider.setCurrentUser("alice@example.com");
        QueueTestEvent event = QueueTestEvent.create("publisher", 0, 0);

        publish(event);

        EventQueueEntry saved = entryByEventId(event.eventId());
        assertThat(saved).isNotNull();
        assertThat(saved.getEventName()).isEqualTo(QueueTestEvent.EVENT_NAME);
        assertThat(saved.getSchemaVersion()).isEqualTo(1);
        assertThat(saved.getRequestId()).isEqualTo("req-publisher-1");
        assertThat(saved.getPublishedBy()).isEqualTo("alice@example.com");
        assertThat(saved.getPublishedAt()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(EventQueueEntryStatus.NEW);
        assertJsonEquals(
                """
                {
                  "eventId": "%s",
                  "key": "publisher",
                  "failAttempts": 0,
                  "processingDelayMs": 0
                }
                """
                        .formatted(event.eventId()),
                saved.getPayload());
    }
}
