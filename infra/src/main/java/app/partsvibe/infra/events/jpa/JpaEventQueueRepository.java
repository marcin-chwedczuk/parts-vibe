package app.partsvibe.infra.events.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaEventQueueRepository implements EventQueueRepository {
    private static final Logger log = LoggerFactory.getLogger(JpaEventQueueRepository.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public EventQueueEntry save(EventQueueEntry entry) {
        if (entry.getId() == null) {
            // TODO: Will this set proper ID? Verify
            entityManager.persist(entry);
            return entry;
        }
        return entityManager.merge(entry);
    }

    @Override
    @Transactional
    public int markDone(long id, Instant now) {
        // TODO: Can be just operation on entire entity, use Spring Data repo
        return entityManager
                .createQuery(
                        """
                        UPDATE EventQueueEntry e
                        SET e.status = :doneStatus,
                            e.lockedAt = NULL,
                            e.lockedBy = NULL,
                            e.lastError = NULL,
                            e.updatedAt = :now
                        WHERE e.id = :id
                          AND e.status = :processingStatus
                        """)
                .setParameter("doneStatus", EventQueueStatus.DONE)
                .setParameter("processingStatus", EventQueueStatus.PROCESSING)
                .setParameter("id", id)
                .setParameter("now", now)
                .executeUpdate();
    }

    @Override
    @Transactional
    public int markFailed(long id, Instant nextAttemptAt, String lastError, Instant now) {
        return entityManager
                .createQuery(
                        """
                        UPDATE EventQueueEntry e
                        SET e.status = :failedStatus,
                            e.nextAttemptAt = :nextAttemptAt,
                            e.lastError = :lastError,
                            e.lockedAt = NULL,
                            e.lockedBy = NULL,
                            e.updatedAt = :now
                        WHERE e.id = :id
                          AND e.status = :processingStatus
                        """)
                .setParameter("failedStatus", EventQueueStatus.FAILED)
                .setParameter("processingStatus", EventQueueStatus.PROCESSING)
                .setParameter("id", id)
                .setParameter("nextAttemptAt", nextAttemptAt)
                .setParameter("lastError", lastError)
                .setParameter("now", now)
                .executeUpdate();
    }

    @Override
    @Transactional
    public int releaseForRetry(long id, Instant nextAttemptAt, Instant now) {
        return entityManager
                .createQuery(
                        """
                        UPDATE EventQueueEntry e
                        SET e.status = :newStatus,
                            e.nextAttemptAt = :nextAttemptAt,
                            e.lockedAt = NULL,
                            e.lockedBy = NULL,
                            e.updatedAt = :now
                        WHERE e.id = :id
                          AND e.status = :processingStatus
                        """)
                .setParameter("newStatus", EventQueueStatus.NEW)
                .setParameter("processingStatus", EventQueueStatus.PROCESSING)
                .setParameter("id", id)
                .setParameter("nextAttemptAt", nextAttemptAt)
                .setParameter("now", now)
                .executeUpdate();
    }

    @Override
    @Transactional
    public int requeueStaleProcessing(Instant lockedBefore, Instant now) {
        return entityManager
                .createQuery(
                        """
                        UPDATE EventQueueEntry e
                        SET e.status = :failedStatus,
                            e.nextAttemptAt = :now,
                            e.lastError = :lastError,
                            e.lockedAt = NULL,
                            e.lockedBy = NULL,
                            e.updatedAt = :now
                        WHERE e.status = :processingStatus
                          AND e.lockedAt < :lockedBefore
                        """)
                .setParameter("failedStatus", EventQueueStatus.FAILED)
                .setParameter("processingStatus", EventQueueStatus.PROCESSING)
                .setParameter("lastError", "Processing lock timeout reached.")
                .setParameter("lockedBefore", lockedBefore)
                .setParameter("now", now)
                .executeUpdate();
    }

    @Override
    @Transactional
    public int deleteDoneOlderThan(Instant cutoff, int limit) {
        if (limit <= 0) {
            return 0;
        }
        return entityManager
                .createNativeQuery(
                        """
                        DELETE FROM event_queue
                        WHERE id IN (
                            SELECT id
                            FROM event_queue
                            WHERE status = 'DONE'
                              AND updated_at < :cutoff
                            ORDER BY id
                            LIMIT :limit
                        )
                        """)
                .setParameter("cutoff", cutoff)
                .setParameter("limit", limit)
                .executeUpdate();
    }

    @Override
    @Transactional
    public int deleteFailedOlderThan(Instant cutoff, int limit) {
        if (limit <= 0) {
            return 0;
        }
        return entityManager
                .createNativeQuery(
                        """
                        DELETE FROM event_queue
                        WHERE id IN (
                            SELECT id
                            FROM event_queue
                            WHERE status = 'FAILED'
                              AND updated_at < :cutoff
                            ORDER BY id
                            LIMIT :limit
                        )
                        """)
                .setParameter("cutoff", cutoff)
                .setParameter("limit", limit)
                .executeUpdate();
    }

    @Override
    @Transactional
    public List<ClaimedEventQueueEntry> claimBatchForProcessing(
            int batchSize, int maxAttempts, String workerId, Instant now) {
        if (batchSize <= 0) {
            log.debug("Claim batch skipped because batchSize <= 0");
            return Collections.emptyList();
        }

        @SuppressWarnings("unchecked")
        List<Number> idRows = entityManager
                .createNativeQuery(
                        """
                        SELECT id
                        FROM event_queue
                        WHERE status IN ('NEW', 'FAILED')
                          AND next_attempt_at <= now()
                          AND attempt_count < :maxAttempts
                        ORDER BY id
                        FOR UPDATE SKIP LOCKED
                        LIMIT :batchSize
                        """)
                .setParameter("maxAttempts", maxAttempts)
                .setParameter("batchSize", batchSize)
                .getResultList();

        if (idRows.isEmpty()) {
            log.debug("No event queue entry IDs selected for claiming");
            return Collections.emptyList();
        }

        List<Long> ids = idRows.stream().map(Number::longValue).toList();

        entityManager
                .createQuery(
                        """
                        UPDATE EventQueueEntry e
                        SET e.status = :processingStatus,
                            e.lockedAt = :now,
                            e.lockedBy = :workerId,
                            e.attemptCount = e.attemptCount + 1,
                            e.updatedAt = :now
                        WHERE e.id IN :ids
                        """)
                .setParameter("processingStatus", EventQueueStatus.PROCESSING)
                .setParameter("now", now)
                .setParameter("workerId", workerId)
                .setParameter("ids", ids)
                .executeUpdate();

        List<ClaimedEventQueueEntry> claimed = entityManager
                .createQuery("SELECT e FROM EventQueueEntry e WHERE e.id IN :ids", EventQueueEntry.class)
                .setParameter("ids", ids)
                .getResultStream()
                .map(ClaimedEventQueueEntry::fromEntity)
                .collect(Collectors.toList());
        log.debug(
                "Claimed event queue rows and marked as PROCESSING. workerId={}, requestedBatchSize={}, selectedCount={}, claimedCount={}",
                workerId,
                batchSize,
                ids.size(),
                claimed.size());
        return claimed;
    }
}
