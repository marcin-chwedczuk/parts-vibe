package app.partsvibe.infra.events;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OutboxEventRepository extends JpaRepository<OutboxEventEntity, Long>, OutboxEventRepositoryCustom {
    Optional<OutboxEventEntity> findByEventId(UUID eventId);

    @Modifying
    @Query(
            value =
                    """
            UPDATE outbox_events
            SET status = 'DONE',
                locked_at = NULL,
                locked_by = NULL,
                last_error = NULL,
                updated_at = :now
            WHERE id = :id
            """,
            nativeQuery = true)
    int markDone(@Param("id") long id, @Param("now") Instant now);

    @Modifying
    @Query(
            value =
                    """
            UPDATE outbox_events
            SET status = 'FAILED',
                next_attempt_at = :nextAttemptAt,
                last_error = :lastError,
                locked_at = NULL,
                locked_by = NULL,
                updated_at = :now
            WHERE id = :id
            """,
            nativeQuery = true)
    int markFailed(
            @Param("id") long id,
            @Param("nextAttemptAt") Instant nextAttemptAt,
            @Param("lastError") String lastError,
            @Param("now") Instant now);

    @Modifying
    @Query(
            value =
                    """
            UPDATE outbox_events
            SET status = 'FAILED',
                next_attempt_at = :now,
                last_error = 'Processing lock timeout reached.',
                locked_at = NULL,
                locked_by = NULL,
                updated_at = :now
            WHERE status = 'PROCESSING'
              AND locked_at < :lockedBefore
            """,
            nativeQuery = true)
    int requeueStaleProcessing(@Param("lockedBefore") Instant lockedBefore, @Param("now") Instant now);
}
