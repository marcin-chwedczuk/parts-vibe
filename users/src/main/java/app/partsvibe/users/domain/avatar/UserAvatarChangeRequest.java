package app.partsvibe.users.domain.avatar;

import app.partsvibe.shared.persistence.BaseAuditableEntity;
import app.partsvibe.users.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "user_avatar_change_requests",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_user_avatar_change_requests_new_avatar_file_id",
                    columnNames = "new_avatar_file_id")
        })
@SequenceGenerator(
        name = BaseAuditableEntity.ID_GENERATOR_NAME,
        sequenceName = "user_avatar_change_requests_id_seq",
        allocationSize = BaseAuditableEntity.ID_ALLOCATION_SIZE)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserAvatarChangeRequest extends BaseAuditableEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @Column(name = "new_avatar_file_id", nullable = false, updatable = false)
    private UUID newAvatarFileId;

    @Column(name = "previous_avatar_file_id")
    private UUID previousAvatarFileId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private UserAvatarChangeRequestStatus status;

    @NotNull
    @Column(name = "requested_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    private Instant requestedAt;

    @Column(name = "resolved_at", columnDefinition = "timestamp with time zone")
    private Instant resolvedAt;

    public UserAvatarChangeRequest(User user, UUID newAvatarFileId, UUID previousAvatarFileId, Instant requestedAt) {
        this.user = user;
        this.newAvatarFileId = newAvatarFileId;
        this.previousAvatarFileId = previousAvatarFileId;
        this.requestedAt = requestedAt;
        this.status = UserAvatarChangeRequestStatus.PENDING;
    }
}
