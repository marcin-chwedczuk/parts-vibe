package app.partsvibe.infra.events;

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
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "outbox_events",
        uniqueConstraints = @UniqueConstraint(name = "uk_outbox_events_event_id", columnNames = "event_id"),
        indexes = {
            @Index(name = "idx_outbox_events_status_next_attempt_id", columnList = "status,next_attempt_at,id"),
            @Index(name = "idx_outbox_events_occurred_at", columnList = "occurred_at")
        })
public class OutboxEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
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

    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private OutboxEventStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant nextAttemptAt;

    @Column(name = "locked_at", columnDefinition = "timestamp with time zone")
    private Instant lockedAt;

    @Column(name = "locked_by", length = 120)
    private String lockedBy;

    @Column(name = "last_error", length = 2000)
    private String lastError;

    @Column(name = "created_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant updatedAt;

    protected OutboxEventEntity() {}

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

    public static OutboxEventEntity newEvent(
            UUID eventId, String eventType, int schemaVersion, Instant occurredAt, String requestId, String payload) {
        OutboxEventEntity entity = new OutboxEventEntity();
        entity.eventId = eventId;
        entity.eventType = eventType;
        entity.schemaVersion = schemaVersion;
        entity.occurredAt = occurredAt;
        entity.requestId = requestId;
        entity.payload = payload;
        entity.status = OutboxEventStatus.NEW;
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

    public OutboxEventStatus getStatus() {
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
