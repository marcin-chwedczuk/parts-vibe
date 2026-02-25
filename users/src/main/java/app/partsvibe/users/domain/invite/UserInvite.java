package app.partsvibe.users.domain.invite;

import app.partsvibe.shared.persistence.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "user_invites",
        uniqueConstraints = {@UniqueConstraint(name = "uk_user_invites_token_hash", columnNames = "token_hash")})
@SequenceGenerator(
        name = BaseAuditableEntity.ID_GENERATOR_NAME,
        sequenceName = "user_invites_id_seq",
        allocationSize = BaseAuditableEntity.ID_ALLOCATION_SIZE)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserInvite extends BaseAuditableEntity {
    @NotBlank
    @Email
    @Size(max = 64)
    @Column(nullable = false, length = 64)
    private String email;

    @NotBlank
    @Size(max = 32)
    @Column(name = "role_name", nullable = false, length = 32)
    private String roleName;

    @Size(max = 1000)
    @Column(name = "invite_message", length = 1000)
    private String inviteMessage;

    @NotBlank
    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @NotNull
    @Column(name = "expires_at", nullable = false, columnDefinition = "timestamp with time zone")
    private Instant expiresAt;

    @Column(name = "used_at", columnDefinition = "timestamp with time zone")
    private Instant usedAt;

    @Column(name = "revoked_at", columnDefinition = "timestamp with time zone")
    private Instant revokedAt;

    public UserInvite(String email, String roleName, String inviteMessage, String tokenHash, Instant expiresAt) {
        this.email = email;
        this.roleName = roleName;
        this.inviteMessage = inviteMessage;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isActiveAt(Instant now) {
        return usedAt == null && revokedAt == null && expiresAt.isAfter(now);
    }
}
