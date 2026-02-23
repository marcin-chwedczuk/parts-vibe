package app.partsvibe.infra.events.jpa;

import app.partsvibe.shared.persistence.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnTransformer;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(
        name = "event_queue",
        uniqueConstraints = @UniqueConstraint(name = "uk_event_queue_event_id", columnNames = "event_id"),
        indexes = {
            @Index(name = "idx_event_queue_status_next_attempt_id", columnList = "status,next_attempt_at,id"),
            @Index(name = "idx_event_queue_published_at", columnList = "published_at")
        })
@SequenceGenerator(
        name = BaseEntity.ID_GENERATOR_NAME,
        sequenceName = "event_queue_id_seq",
        allocationSize = BaseEntity.ID_ALLOCATION_SIZE)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventQueueEntry extends BaseEntity {
    public static final int LAST_ERROR_MAX_LENGTH = 2000;

    @Column(name = "event_id", nullable = false, updatable = false)
    private UUID eventId;

    @Column(name = "event_name", nullable = false, length = 120)
    private String eventName;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @CreatedDate
    @Column(name = "published_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant publishedAt;

    @CreatedBy
    @Column(name = "published_by", nullable = false, length = 120)
    private String publishedBy;

    // TODO: Verify if this is correct approach, looks like we are casting to jsonb on DB side
    @JdbcTypeCode(SqlTypes.JSON)
    @ColumnTransformer(write = "?::jsonb")
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private EventQueueEntryStatus status;

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

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant updatedAt;

    public static EventQueueEntry newEvent(
            UUID eventId,
            String eventName,
            int schemaVersion,
            String requestId,
            Instant publishedAt,
            String publishedBy,
            String payload) {
        EventQueueEntry entity = new EventQueueEntry();
        entity.eventId = eventId;
        entity.eventName = eventName;
        entity.schemaVersion = schemaVersion;
        entity.requestId = requestId;
        entity.publishedAt = publishedAt;
        entity.publishedBy = publishedBy;
        entity.payload = payload;
        entity.status = EventQueueEntryStatus.NEW;
        entity.attemptCount = 0;
        entity.nextAttemptAt = Instant.now();
        return entity;
    }
}
