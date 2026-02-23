package app.partsvibe.users.commands.profile;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.cqrs.NoResult;
import app.partsvibe.storage.api.StorageClient;
import app.partsvibe.storage.api.StorageException;
import app.partsvibe.storage.api.StorageObjectType;
import app.partsvibe.storage.api.StorageUploadRequest;
import app.partsvibe.users.errors.UserNotFoundException;
import app.partsvibe.users.repo.UserRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class UploadCurrentUserAvatarCommandHandler extends BaseCommandHandler<UploadCurrentUserAvatarCommand, NoResult> {
    private static final Logger log = LoggerFactory.getLogger(UploadCurrentUserAvatarCommandHandler.class);

    private final UserRepository userRepository;
    private final StorageClient storageClient;

    UploadCurrentUserAvatarCommandHandler(UserRepository userRepository, StorageClient storageClient) {
        this.userRepository = userRepository;
        this.storageClient = storageClient;
    }

    @Override
    protected NoResult doHandle(UploadCurrentUserAvatarCommand command) {
        var user = userRepository
                .findByUsername(command.username())
                .orElseThrow(() -> new UserNotFoundException(command.username()));

        var uploadResult = storageClient.upload(new StorageUploadRequest(
                StorageObjectType.USER_AVATAR_IMAGE, command.originalFilename(), command.content()));

        UUID previousAvatarId = user.getAvatarId();
        user.setAvatarId(uploadResult.fileId());
        userRepository.save(user);

        if (previousAvatarId != null && !previousAvatarId.equals(uploadResult.fileId())) {
            try {
                storageClient.delete(previousAvatarId);
                log.info(
                        "Previous avatar deleted after successful profile avatar upload. username={}, previousAvatarId={}",
                        command.username(),
                        previousAvatarId);
            } catch (StorageException ex) {
                // Best-effort cleanup; current avatar is already set.
                log.warn(
                        "Failed to delete previous avatar after profile upload (best-effort). username={}, previousAvatarId={}",
                        command.username(),
                        previousAvatarId);
            }
        }

        return NoResult.INSTANCE;
    }
}
