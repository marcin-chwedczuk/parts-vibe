package app.partsvibe.infra.events;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;

public class OutboxEventRepositoryImpl implements OutboxEventRepositoryCustom {
    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public List<ClaimedOutboxEvent> claimBatchForProcessing(int batchSize, int maxAttempts, String workerId) {
        if (batchSize <= 0) {
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
            return Collections.emptyList();
        }

        List<Long> ids = idRows.stream().map(Number::longValue).toList();
        Instant now = Instant.now();

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

        return entityManager
                .createQuery("SELECT e FROM OutboxEventEntity e WHERE e.id IN :ids", OutboxEventEntity.class)
                .setParameter("ids", ids)
                .getResultStream()
                .map(ClaimedOutboxEvent::fromEntity)
                .collect(Collectors.toList());
    }
}
