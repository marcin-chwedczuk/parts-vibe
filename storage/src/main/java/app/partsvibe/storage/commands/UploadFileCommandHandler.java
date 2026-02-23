package app.partsvibe.storage.commands;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.events.publishing.EventPublisher;
import app.partsvibe.shared.security.CurrentUserProvider;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.storage.api.StorageUploadResult;
import app.partsvibe.storage.api.events.FileUploadedEvent;
import app.partsvibe.storage.domain.StoredFile;
import app.partsvibe.storage.domain.StoredFileKind;
import app.partsvibe.storage.repo.StoredFileRepository;
import app.partsvibe.storage.service.FilesystemStorage;
import app.partsvibe.storage.service.StorageRules;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class UploadFileCommandHandler extends BaseCommandHandler<UploadFileCommand, StorageUploadResult> {
    private static final String SYSTEM_UPLOADER = "system";

    private final StoredFileRepository storedFileRepository;
    private final FilesystemStorage filesystemStorage;
    private final StorageRules storageRules;
    private final EventPublisher eventPublisher;
    private final CurrentUserProvider currentUserProvider;
    private final TimeProvider timeProvider;

    UploadFileCommandHandler(
            StoredFileRepository storedFileRepository,
            FilesystemStorage filesystemStorage,
            StorageRules storageRules,
            EventPublisher eventPublisher,
            CurrentUserProvider currentUserProvider,
            TimeProvider timeProvider) {
        this.storedFileRepository = storedFileRepository;
        this.filesystemStorage = filesystemStorage;
        this.storageRules = storageRules;
        this.eventPublisher = eventPublisher;
        this.currentUserProvider = currentUserProvider;
        this.timeProvider = timeProvider;
    }

    @Override
    protected StorageUploadResult doHandle(UploadFileCommand command) {
        String originalFilename = command.originalFilename().trim();
        byte[] content = command.content();
        storageRules.validateUpload(command.objectType(), originalFilename, content.length);

        UUID fileId = UUID.randomUUID();
        StoredFileKind kind = command.objectType().isImage() ? StoredFileKind.IMAGE : StoredFileKind.BLOB;

        StoredFile storedFile = new StoredFile(
                fileId,
                command.objectType(),
                kind,
                normalizeFilename(originalFilename),
                content.length,
                timeProvider.now(),
                currentUserProvider.currentUsername().orElse(SYSTEM_UPLOADER));

        storedFileRepository.save(storedFile);
        filesystemStorage.writeBlob(fileId, content);

        FileUploadedEvent event = FileUploadedEvent.create(fileId, command.objectType());
        eventPublisher.publish(event);

        return new StorageUploadResult(fileId);
    }

    private static String normalizeFilename(String originalFilename) {
        return originalFilename.replace('\\', '/').replaceAll(".*/", "");
    }
}
