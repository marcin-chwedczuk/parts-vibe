package app.partsvibe.storage.commands;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.cqrs.NoResult;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.storage.domain.StoredFileStatus;
import app.partsvibe.storage.errors.StoredFileNotFoundException;
import app.partsvibe.storage.repo.StoredFileRepository;
import app.partsvibe.storage.service.FilesystemStorage;
import org.springframework.stereotype.Component;

@Component
class DeleteStoredFileCommandHandler extends BaseCommandHandler<DeleteStoredFileCommand, NoResult> {
    private final StoredFileRepository storedFileRepository;
    private final FilesystemStorage filesystemStorage;
    private final TimeProvider timeProvider;

    DeleteStoredFileCommandHandler(
            StoredFileRepository storedFileRepository, FilesystemStorage filesystemStorage, TimeProvider timeProvider) {
        this.storedFileRepository = storedFileRepository;
        this.filesystemStorage = filesystemStorage;
        this.timeProvider = timeProvider;
    }

    @Override
    protected NoResult doHandle(DeleteStoredFileCommand command) {
        var storedFile = storedFileRepository
                .findByFileId(command.fileId())
                .orElseThrow(() -> new StoredFileNotFoundException(command.fileId()));

        if (storedFile.getStatus() == StoredFileStatus.DELETED) {
            return NoResult.INSTANCE;
        }

        filesystemStorage.deleteFileDirectory(storedFile.getFileId());
        storedFile.setStatus(StoredFileStatus.DELETED);
        storedFile.setDeletedAt(timeProvider.now());
        storedFile.setThumbnail128Ready(false);
        storedFile.setThumbnail512Ready(false);
        storedFileRepository.save(storedFile);

        return NoResult.INSTANCE;
    }
}
