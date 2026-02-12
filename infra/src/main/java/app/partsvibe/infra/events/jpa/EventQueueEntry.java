package app.partsvibe.infra.events.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "event_queue",
        uniqueConstraints = @UniqueConstraint(name = "uk_event_queue_event_id", columnNames = "event_id"),
        indexes = {
            @Index(name = "idx_event_queue_status_next_attempt_id", columnList = "status,next_attempt_at,id"),
            @Index(name = "idx_event_queue_occurred_at", columnList = "occurred_at")
        })
public class EventQueueEntry {
    public static final int LAST_ERROR_MAX_LENGTH = 2000;

    @Id
    // TODO: Investigate why event_queue.id DB schema is inconsistent with IDENTITY generation in local env.
    // IDENTITY currently causes INSERT failures with id=NULL, while this sequence mapping works reliably.
    // Align DB DDL/migrations so we can use one consistent strategy across entities.
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "event_queue_id_seq_gen")
    @SequenceGenerator(name = "event_queue_id_seq_gen", sequenceName = "event_queue_id_seq", allocationSize = 1)
    private Long id;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "occurred_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant occurredAt;

    @Column(name = "request_id", nullable = false, length = 64)
    private String requestId;

    // TODO: Verify if this is correct approach, looks like we are casting to jsonb on DB side
    @JdbcTypeCode(SqlTypes.JSON)
    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private EventQueueStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant nextAttemptAt;

    @Column(name = "locked_at", columnDefinition = "timestamp with time zone")
    private Instant lockedAt;

    @Column(name = "locked_by", length = 120)
    private String lockedBy;

    @Column(name = "last_error", length = LAST_ERROR_MAX_LENGTH)
    private String lastError;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant updatedAt;

    protected EventQueueEntry() {}

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public static EventQueueEntry newEvent(
            UUID eventId, String eventType, int schemaVersion, Instant occurredAt, String requestId, String payload) {
        EventQueueEntry entity = new EventQueueEntry();
        entity.eventId = eventId;
        entity.eventType = eventType;
        entity.schemaVersion = schemaVersion;
        entity.occurredAt = occurredAt;
        entity.requestId = requestId;
        entity.payload = payload;
        entity.status = EventQueueStatus.NEW;
        entity.attemptCount = 0;
        entity.nextAttemptAt = Instant.now();
        return entity;
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getPayload() {
        return payload;
    }

    public EventQueueStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getLockedAt() {
        return lockedAt;
    }

    public String getLockedBy() {
        return lockedBy;
    }

    public String getLastError() {
        return lastError;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
