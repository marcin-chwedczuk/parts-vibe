package app.partsvibe.users.commands.profile;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.cqrs.NoResult;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.storage.api.StorageClient;
import app.partsvibe.storage.api.StorageObjectType;
import app.partsvibe.storage.api.StorageUploadRequest;
import app.partsvibe.users.domain.avatar.UserAvatarChangeRequest;
import app.partsvibe.users.domain.avatar.UserAvatarChangeRequestStatus;
import app.partsvibe.users.errors.UserNotFoundException;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.avatar.UserAvatarChangeRequestRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ChangeCurrentUserAvatarCommandHandler extends BaseCommandHandler<ChangeCurrentUserAvatarCommand, NoResult> {
    private final UserRepository userRepository;
    private final UserAvatarChangeRequestRepository avatarChangeRequestRepository;
    private final StorageClient storageClient;
    private final TimeProvider timeProvider;

    ChangeCurrentUserAvatarCommandHandler(
            UserRepository userRepository,
            UserAvatarChangeRequestRepository avatarChangeRequestRepository,
            StorageClient storageClient,
            TimeProvider timeProvider) {
        this.userRepository = userRepository;
        this.avatarChangeRequestRepository = avatarChangeRequestRepository;
        this.storageClient = storageClient;
        this.timeProvider = timeProvider;
    }

    @Override
    protected NoResult doHandle(ChangeCurrentUserAvatarCommand command) {
        var user = userRepository
                .findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        var uploadResult = storageClient.upload(new StorageUploadRequest(
                StorageObjectType.USER_AVATAR_IMAGE, command.originalFilename(), command.content()));

        var pendingRequests = avatarChangeRequestRepository.findByUserIdAndStatus(
                user.getId(), UserAvatarChangeRequestStatus.PENDING);
        for (var pendingRequest : pendingRequests) {
            pendingRequest.setStatus(UserAvatarChangeRequestStatus.SUPERSEDED);
            pendingRequest.setResolvedAt(timeProvider.now());
            avatarChangeRequestRepository.save(pendingRequest);
            storageClient.delete(pendingRequest.getNewAvatarFileId());
        }

        UUID previousAvatarId = user.getAvatarId();
        var request = new UserAvatarChangeRequest(user, uploadResult.fileId(), previousAvatarId, timeProvider.now());
        avatarChangeRequestRepository.save(request);

        return NoResult.INSTANCE;
    }
}
