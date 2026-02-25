package app.partsvibe.users.repo.security;

import app.partsvibe.users.domain.security.UserPasswordResetToken;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserPasswordResetTokenRepository extends JpaRepository<UserPasswordResetToken, Long> {
    Optional<UserPasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query(
            """
            update UserPasswordResetToken t
            set t.revokedAt = :now
            where t.user.id = :userId
              and t.usedAt is null
              and t.revokedAt is null
              and t.expiresAt > :now
            """)
    int revokeActiveTokensByUserId(@Param("userId") Long userId, @Param("now") Instant now);
}
