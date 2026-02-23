package app.partsvibe.storage.commands;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.storage.api.DeleteFileResult;
import app.partsvibe.storage.domain.StoredFileStatus;
import app.partsvibe.storage.repo.StoredFileRepository;
import app.partsvibe.storage.service.FilesystemStorage;
import org.springframework.stereotype.Component;

@Component
class DeleteFileCommandHandler extends BaseCommandHandler<DeleteFileCommand, DeleteFileResult> {
    private final StoredFileRepository storedFileRepository;
    private final FilesystemStorage filesystemStorage;
    private final TimeProvider timeProvider;

    DeleteFileCommandHandler(
            StoredFileRepository storedFileRepository, FilesystemStorage filesystemStorage, TimeProvider timeProvider) {
        this.storedFileRepository = storedFileRepository;
        this.filesystemStorage = filesystemStorage;
        this.timeProvider = timeProvider;
    }

    @Override
    protected DeleteFileResult doHandle(DeleteFileCommand command) {
        var storedFile = storedFileRepository.findByFileId(command.fileId()).orElse(null);
        if (storedFile == null) {
            log.info("Storage delete skipped; file not found. fileId={}", command.fileId());
            return DeleteFileResult.notFound();
        }

        if (storedFile.getStatus() == StoredFileStatus.DELETED) {
            log.info("Storage delete skipped; file already marked deleted. fileId={}", command.fileId());
            return DeleteFileResult.notFound();
        }

        filesystemStorage.deleteFileDirectory(storedFile.getFileId());
        storedFile.setStatus(StoredFileStatus.DELETED);
        storedFile.setDeletedAt(timeProvider.now());
        storedFile.setThumbnail128Ready(false);
        storedFile.setThumbnail512Ready(false);
        storedFileRepository.save(storedFile);
        log.info("Storage file deleted. fileId={}", command.fileId());

        return DeleteFileResult.deleted();
    }
}
