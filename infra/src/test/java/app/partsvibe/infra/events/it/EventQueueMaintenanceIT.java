package app.partsvibe.infra.events.it;

import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.infra.events.handling.EventQueueRetentionCleanupJob;
import app.partsvibe.infra.events.handling.EventQueueTimedOutProcessingRecoveryJob;
import app.partsvibe.infra.events.it.support.QueueTestEvent;
import app.partsvibe.infra.events.jpa.EventQueueEntry;
import app.partsvibe.infra.events.jpa.EventQueueEntryStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventQueueMaintenanceIT extends AbstractEventQueueDatabaseIntegrationTest {
    private static final String WORKER_ID = "maintenance-worker";

    @Autowired
    private EventQueueTimedOutProcessingRecoveryJob eventQueueTimedOutProcessingRecoveryJob;

    @Autowired
    private EventQueueRetentionCleanupJob eventQueueRetentionCleanupJob;

    @Test
    void repositoryRecoversOnlyStaleProcessingEntries() {
        Instant now = Instant.now();
        EventQueueEntry stale = inTx(() -> eventQueueRepository.save(newEntry("stale-processing", now)));
        EventQueueEntry fresh = inTx(() -> eventQueueRepository.save(newEntry("fresh-processing", now)));
        EventQueueEntry untouched = inTx(() -> eventQueueRepository.save(newEntry("untouched-new", now)));

        inTx(() -> {
            EventQueueEntry staleManaged = entityManager.find(EventQueueEntry.class, stale.getId());
            staleManaged.setStatus(EventQueueEntryStatus.PROCESSING);
            staleManaged.setLockedBy(WORKER_ID);
            staleManaged.setLockedAt(now.minusSeconds(30));

            EventQueueEntry freshManaged = entityManager.find(EventQueueEntry.class, fresh.getId());
            freshManaged.setStatus(EventQueueEntryStatus.PROCESSING);
            freshManaged.setLockedBy(WORKER_ID);
            freshManaged.setLockedAt(now.minusMillis(200));
            return null;
        });

        int recovered = inTx(() -> eventQueueRepository.recoverTimedOutProcessingEntries(now.minusSeconds(2), now));
        assertThat(recovered).isEqualTo(1);

        EventQueueEntry staleAfter = entryByEventId(stale.getEventId());
        assertThat(staleAfter.getStatus()).isEqualTo(EventQueueEntryStatus.FAILED);
        assertThat(staleAfter.getLockedAt()).isNull();
        assertThat(staleAfter.getLockedBy()).isNull();
        assertThat(staleAfter.getLastError()).isEqualTo("Processing lock timeout reached.");

        EventQueueEntry freshAfter = entryByEventId(fresh.getEventId());
        assertThat(freshAfter.getStatus()).isEqualTo(EventQueueEntryStatus.PROCESSING);
        assertThat(freshAfter.getLockedBy()).isEqualTo(WORKER_ID);

        EventQueueEntry untouchedAfter = entryByEventId(untouched.getEventId());
        assertThat(untouchedAfter.getStatus()).isEqualTo(EventQueueEntryStatus.NEW);
    }

    @Test
    void recoveryJobMarksTimedOutProcessingEntriesAsFailed() {
        Instant now = Instant.now();
        EventQueueEntry stale = inTx(() -> eventQueueRepository.save(newEntry("job-stale-processing", now)));
        EventQueueEntry fresh = inTx(() -> eventQueueRepository.save(newEntry("job-fresh-processing", now)));

        inTx(() -> {
            EventQueueEntry staleManaged = entityManager.find(EventQueueEntry.class, stale.getId());
            staleManaged.setStatus(EventQueueEntryStatus.PROCESSING);
            staleManaged.setLockedBy(WORKER_ID);
            staleManaged.setLockedAt(now.minusSeconds(30));

            EventQueueEntry freshManaged = entityManager.find(EventQueueEntry.class, fresh.getId());
            freshManaged.setStatus(EventQueueEntryStatus.PROCESSING);
            freshManaged.setLockedBy(WORKER_ID);
            freshManaged.setLockedAt(now.minusMillis(200));
            return null;
        });

        eventQueueTimedOutProcessingRecoveryJob.recoverTimedOutProcessing();

        EventQueueEntry staleAfter = entryByEventId(stale.getEventId());
        assertThat(staleAfter.getStatus()).isEqualTo(EventQueueEntryStatus.FAILED);
        assertThat(staleAfter.getLockedAt()).isNull();
        assertThat(staleAfter.getLockedBy()).isNull();

        EventQueueEntry freshAfter = entryByEventId(fresh.getEventId());
        assertThat(freshAfter.getStatus()).isEqualTo(EventQueueEntryStatus.PROCESSING);
    }

    @Test
    void retentionCleanupJobDeletesOnlyExpiredDoneAndFailedEntries() {
        Instant now = Instant.now();
        EventQueueEntry doneExpired = inTx(() -> eventQueueRepository.save(newEntry("done-expired", now)));
        EventQueueEntry doneRecent = inTx(() -> eventQueueRepository.save(newEntry("done-recent", now)));
        EventQueueEntry failedExpired = inTx(() -> eventQueueRepository.save(newEntry("failed-expired", now)));
        EventQueueEntry failedRecent = inTx(() -> eventQueueRepository.save(newEntry("failed-recent", now)));
        EventQueueEntry newOld = inTx(() -> eventQueueRepository.save(newEntry("new-old", now)));

        inTx(() -> {
            updateStatusAndUpdatedAt(doneExpired.getId(), EventQueueEntryStatus.DONE, now.minus(Duration.ofDays(40)));
            updateStatusAndUpdatedAt(doneRecent.getId(), EventQueueEntryStatus.DONE, now.minus(Duration.ofDays(2)));
            updateStatusAndUpdatedAt(
                    failedExpired.getId(), EventQueueEntryStatus.FAILED, now.minus(Duration.ofDays(100)));
            updateStatusAndUpdatedAt(failedRecent.getId(), EventQueueEntryStatus.FAILED, now.minus(Duration.ofDays(2)));
            updateStatusAndUpdatedAt(newOld.getId(), EventQueueEntryStatus.NEW, now.minus(Duration.ofDays(200)));
            return null;
        });

        eventQueueRetentionCleanupJob.cleanup();

        assertThat(exists(doneExpired.getEventId())).isFalse();
        assertThat(exists(failedExpired.getEventId())).isFalse();
        assertThat(exists(doneRecent.getEventId())).isTrue();
        assertThat(exists(failedRecent.getEventId())).isTrue();
        assertThat(exists(newOld.getEventId())).isTrue();
    }

    private boolean exists(UUID eventId) {
        return inTx(() -> !entityManager
                .createQuery("SELECT e.id FROM EventQueueEntry e WHERE e.eventId = :eventId", Long.class)
                .setParameter("eventId", eventId)
                .getResultList()
                .isEmpty());
    }

    private void updateStatusAndUpdatedAt(long id, EventQueueEntryStatus status, Instant updatedAt) {
        entityManager
                .createNativeQuery("UPDATE event_queue SET status = :status, updated_at = :updatedAt WHERE id = :id")
                .setParameter("status", status.name())
                .setParameter("updatedAt", updatedAt)
                .setParameter("id", id)
                .executeUpdate();
    }

    private static EventQueueEntry newEntry(String key, Instant now) {
        UUID eventId = UUID.randomUUID();
        return EventQueueEntry.newEvent(
                eventId,
                QueueTestEvent.EVENT_NAME,
                1,
                "req-maintenance",
                now,
                "maintenance-tester",
                """
                {"eventId":"%s","key":"%s","failAttempts":0,"processingDelayMs":0}
                """
                        .formatted(eventId, key));
    }
}
