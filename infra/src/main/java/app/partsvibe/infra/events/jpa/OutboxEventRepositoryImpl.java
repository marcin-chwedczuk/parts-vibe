package app.partsvibe.infra.events.jpa;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class OutboxEventRepositoryImpl implements OutboxEventRepositoryCustom {
    private static final Logger log = LoggerFactory.getLogger(OutboxEventRepositoryImpl.class);

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public List<ClaimedOutboxEvent> claimBatchForProcessing(
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
                        FROM outbox_events
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
            log.debug("No outbox event IDs selected for claiming");
            return Collections.emptyList();
        }

        List<Long> ids = idRows.stream().map(Number::longValue).toList();

        entityManager
                .createNativeQuery(
                        """
                        UPDATE outbox_events
                        SET status = 'PROCESSING',
                            locked_at = :now,
                            locked_by = :workerId,
                            attempt_count = attempt_count + 1,
                            updated_at = :now
                        WHERE id IN (:ids)
                        """)
                .setParameter("now", now)
                .setParameter("workerId", workerId)
                .setParameter("ids", ids)
                .executeUpdate();

        List<ClaimedOutboxEvent> claimed = entityManager
                .createQuery("SELECT e FROM OutboxEventEntity e WHERE e.id IN :ids", OutboxEventEntity.class)
                .setParameter("ids", ids)
                .getResultStream()
                .map(ClaimedOutboxEvent::fromEntity)
                .collect(Collectors.toList());
        log.debug(
                "Claimed outbox rows and marked as PROCESSING. workerId={}, requestedBatchSize={}, selectedCount={}, claimedCount={}",
                workerId,
                batchSize,
                ids.size(),
                claimed.size());
        return claimed;
    }
}
