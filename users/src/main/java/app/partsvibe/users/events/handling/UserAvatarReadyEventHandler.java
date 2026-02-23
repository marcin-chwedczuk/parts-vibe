package app.partsvibe.users.events.handling;

import app.partsvibe.shared.events.handling.BaseEventHandler;
import app.partsvibe.shared.events.handling.HandlesEvent;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.storage.api.StorageClient;
import app.partsvibe.storage.api.StorageObjectType;
import app.partsvibe.storage.api.events.FileReadyEvent;
import app.partsvibe.users.domain.avatar.UserAvatarChangeRequestStatus;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.avatar.UserAvatarChangeRequestRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
@HandlesEvent(name = FileReadyEvent.EVENT_NAME, version = 1)
class UserAvatarReadyEventHandler extends BaseEventHandler<FileReadyEvent> {
    private final UserAvatarChangeRequestRepository avatarChangeRequestRepository;
    private final UserRepository userRepository;
    private final StorageClient storageClient;
    private final TimeProvider timeProvider;

    UserAvatarReadyEventHandler(
            UserAvatarChangeRequestRepository avatarChangeRequestRepository,
            UserRepository userRepository,
            StorageClient storageClient,
            TimeProvider timeProvider) {
        this.avatarChangeRequestRepository = avatarChangeRequestRepository;
        this.userRepository = userRepository;
        this.storageClient = storageClient;
        this.timeProvider = timeProvider;
    }

    @Override
    protected void doHandle(FileReadyEvent event) {
        if (event.objectType() != StorageObjectType.USER_AVATAR_IMAGE) {
            return;
        }

        var requestOpt = avatarChangeRequestRepository.findByNewAvatarFileId(event.fileId());
        if (requestOpt.isEmpty()) {
            return;
        }

        var request = requestOpt.get();
        if (request.getStatus() != UserAvatarChangeRequestStatus.PENDING) {
            return;
        }

        var user = request.getUser();
        user.setAvatarId(request.getNewAvatarFileId());
        userRepository.save(user);

        request.setStatus(UserAvatarChangeRequestStatus.APPLIED);
        request.setResolvedAt(timeProvider.now());
        avatarChangeRequestRepository.save(request);

        UUID previousAvatarFileId = request.getPreviousAvatarFileId();
        if (previousAvatarFileId != null && !previousAvatarFileId.equals(request.getNewAvatarFileId())) {
            storageClient.delete(previousAvatarFileId);
        }
    }
}
