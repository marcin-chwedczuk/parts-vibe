package app.partsvibe.users.repo.invite;

import app.partsvibe.users.domain.invite.UserInvite;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserInviteRepository extends JpaRepository<UserInvite, Long> {
    Optional<UserInvite> findByTokenHash(String tokenHash);

    boolean existsByEmailIgnoreCase(String email);

    @Modifying
    @Query(
            """
            update UserInvite i
            set i.revokedAt = :now
            where lower(i.email) = lower(:email)
              and i.usedAt is null
              and i.revokedAt is null
            """)
    int revokeUnconsumedInvitesByEmail(@Param("email") String email, @Param("now") Instant now);

    @Modifying
    @Query(
            """
            update UserInvite i
            set i.revokedAt = :now
            where lower(i.email) = lower(:email)
              and i.id <> :excludeInviteId
              and i.usedAt is null
              and i.revokedAt is null
            """)
    int revokeUnconsumedInvitesByEmailExcludingId(
            @Param("email") String email, @Param("excludeInviteId") Long excludeInviteId, @Param("now") Instant now);
}
