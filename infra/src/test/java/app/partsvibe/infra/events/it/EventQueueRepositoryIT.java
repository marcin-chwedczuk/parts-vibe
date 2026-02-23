package app.partsvibe.infra.events.it;

import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.infra.events.it.support.QueueTestEvent;
import app.partsvibe.infra.events.jpa.EventQueueEntry;
import app.partsvibe.infra.events.jpa.EventQueueEntryStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventQueueRepositoryIT extends AbstractEventQueueDatabaseIntegrationTest {
    @Test
    void claimEntriesForProcessingMarksRowsAsProcessingAndIncrementsAttemptCount() {
        UUID firstEventId = UUID.randomUUID();
        UUID secondEventId = UUID.randomUUID();
        inTx(() -> {
            eventQueueRepository.save(newEntry(firstEventId, "repo-1"));
            eventQueueRepository.save(newEntry(secondEventId, "repo-2"));
            return null;
        });

        List<app.partsvibe.infra.events.jpa.ClaimedEventQueueEntry> claimed =
                inTx(() -> eventQueueRepository.claimEntriesForProcessing(10, 3, "repo-worker", Instant.now()));

        assertThat(claimed).hasSize(2);
        assertThat(claimed).allSatisfy(entry -> assertThat(entry.attemptCount()).isEqualTo(1));

        EventQueueEntry first = entryByEventId(firstEventId);
        EventQueueEntry second = entryByEventId(secondEventId);
        assertThat(first.getStatus()).isEqualTo(EventQueueEntryStatus.PROCESSING);
        assertThat(second.getStatus()).isEqualTo(EventQueueEntryStatus.PROCESSING);
        assertThat(first.getLockedBy()).isEqualTo("repo-worker");
        assertThat(second.getLockedBy()).isEqualTo("repo-worker");
    }

    @Test
    void markEntryAsDoneTransitionsOnlyProcessingRows() {
        UUID eventId = UUID.randomUUID();
        EventQueueEntry entry = inTx(() -> eventQueueRepository.save(newEntry(eventId, "repo-done")));
        inTx(() -> {
            eventQueueRepository.claimEntriesForProcessing(10, 3, "repo-worker", Instant.now());
            return null;
        });

        int updated = inTx(() -> eventQueueRepository.markEntryAsDone(entry.getId(), Instant.now()));
        assertThat(updated).isEqualTo(1);
        assertThat(entryByEventId(eventId).getStatus()).isEqualTo(EventQueueEntryStatus.DONE);

        int updatedAgain = inTx(() -> eventQueueRepository.markEntryAsDone(entry.getId(), Instant.now()));
        assertThat(updatedAgain).isEqualTo(0);
    }

    @Test
    void releaseClaimedEntryMovesRowBackToNew() {
        UUID eventId = UUID.randomUUID();
        EventQueueEntry entry = inTx(() -> eventQueueRepository.save(newEntry(eventId, "repo-release")));
        inTx(() -> {
            eventQueueRepository.claimEntriesForProcessing(10, 3, "repo-worker", Instant.now());
            return null;
        });

        int released =
                inTx(() -> eventQueueRepository.releaseClaimedEntry(entry.getId(), Instant.now(), Instant.now()));
        assertThat(released).isEqualTo(1);

        EventQueueEntry saved = entryByEventId(eventId);
        assertThat(saved.getStatus()).isEqualTo(EventQueueEntryStatus.NEW);
        assertThat(saved.getLockedAt()).isNull();
        assertThat(saved.getLockedBy()).isNull();
    }

    private static EventQueueEntry newEntry(UUID eventId, String key) {
        return EventQueueEntry.newEvent(
                eventId,
                QueueTestEvent.EVENT_NAME,
                1,
                "req-repo",
                Instant.now(),
                "repo-tester",
                """
                {"eventId":"%s","key":"%s","failAttempts":0,"processingDelayMs":0}
                """
                        .formatted(eventId, key));
    }
}
