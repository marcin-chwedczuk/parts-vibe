package app.partsvibe.users.repo.security;

import app.partsvibe.users.domain.security.UserCredentialToken;
import app.partsvibe.users.domain.security.UserCredentialTokenPurpose;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserCredentialTokenRepository extends JpaRepository<UserCredentialToken, Long> {
    Optional<UserCredentialToken> findByTokenHash(String tokenHash);

    boolean existsByUserIdAndPurpose(Long userId, UserCredentialTokenPurpose purpose);

    boolean existsByUserIdAndPurposeAndUsedAtIsNotNull(Long userId, UserCredentialTokenPurpose purpose);

    @Modifying
    @Query(
            """
            update UserCredentialToken t
            set t.revokedAt = :now
            where t.user.id = :userId
              and t.purpose = :purpose
              and t.usedAt is null
              and t.revokedAt is null
              and t.expiresAt > :now
            """)
    int revokeActiveTokensByUserAndPurpose(
            @Param("userId") Long userId,
            @Param("purpose") UserCredentialTokenPurpose purpose,
            @Param("now") Instant now);

    @Modifying
    @Query(
            """
            update UserCredentialToken t
            set t.revokedAt = :now
            where t.user.id = :userId
              and t.purpose = :purpose
              and t.usedAt is null
              and t.revokedAt is null
            """)
    int revokeUnconsumedTokensByUserAndPurpose(
            @Param("userId") Long userId,
            @Param("purpose") UserCredentialTokenPurpose purpose,
            @Param("now") Instant now);
}
