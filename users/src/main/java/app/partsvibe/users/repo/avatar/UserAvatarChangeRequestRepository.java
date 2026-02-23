package app.partsvibe.users.repo.avatar;

import app.partsvibe.users.domain.avatar.UserAvatarChangeRequest;
import app.partsvibe.users.domain.avatar.UserAvatarChangeRequestStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAvatarChangeRequestRepository extends JpaRepository<UserAvatarChangeRequest, Long> {
    Optional<UserAvatarChangeRequest> findByNewAvatarFileId(UUID newAvatarFileId);

    List<UserAvatarChangeRequest> findByUserIdAndStatus(Long userId, UserAvatarChangeRequestStatus status);

    long deleteByUserId(Long userId);
}
