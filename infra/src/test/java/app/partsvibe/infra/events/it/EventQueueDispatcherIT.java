package app.partsvibe.infra.events.it;

import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.infra.events.it.support.QueueTestEvent;
import app.partsvibe.infra.events.jpa.EventQueueEntry;
import app.partsvibe.infra.events.jpa.EventQueueEntryStatus;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

class EventQueueDispatcherIT extends AbstractEventQueueDatabaseIntegrationTest {
    @Test
    void dispatcherRetriesFailedEventAndEventuallyMarksDone() {
        QueueTestEvent event = QueueTestEvent.create("retry", 1, 0);
        publish(event);

        Awaitility.await()
                .atMost(Duration.ofSeconds(8))
                .pollInterval(Duration.ofMillis(20))
                .untilAsserted(() -> {
                    eventQueueDispatcher.pollAndDispatch();
                    assertThat(entryByEventId(event.eventId()).getStatus()).isEqualTo(EventQueueEntryStatus.DONE);
                });

        EventQueueEntry saved = entryByEventId(event.eventId());
        assertThat(saved.getAttemptCount()).isEqualTo(2);
        assertThat(probe.attempts("retry")).isEqualTo(2);
        assertThat(probe.completions("retry")).isEqualTo(1);
    }

    @Test
    void dispatcherProcessesMultipleEventsConcurrently() {
        for (int i = 0; i < 8; i++) {
            publish(QueueTestEvent.create("concurrent-" + i, 0, 200));
        }

        Awaitility.await()
                .atMost(Duration.ofSeconds(12))
                .pollInterval(Duration.ofMillis(20))
                .untilAsserted(() -> {
                    eventQueueDispatcher.pollAndDispatch();
                    assertThat(doneCount()).isEqualTo(8);
                });

        assertThat(probe.totalCompletions()).isEqualTo(8);
        assertThat(probe.maxParallelism()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void dispatcherMarksTimedOutEventAsFailed() {
        QueueTestEvent event = QueueTestEvent.create("timeout", 0, 1_200);
        publish(event);

        Awaitility.await()
                .atMost(Duration.ofSeconds(8))
                .pollInterval(Duration.ofMillis(20))
                .untilAsserted(() -> {
                    eventQueueDispatcher.pollAndDispatch();
                    EventQueueEntry saved = entryByEventId(event.eventId());
                    assertThat(saved.getStatus()).isEqualTo(EventQueueEntryStatus.FAILED);
                    assertThat(saved.getAttemptCount()).isEqualTo(3);
                });

        EventQueueEntry saved = entryByEventId(event.eventId());
        assertThat(saved.getAttemptCount()).isEqualTo(3);
        assertThat(probe.completions("timeout")).isEqualTo(0);
    }
}
