package app.partsvibe.storage.events.handling;

import app.partsvibe.shared.events.handling.BaseEventHandler;
import app.partsvibe.shared.events.handling.HandlesEvent;
import app.partsvibe.storage.api.events.FileReadyEvent;
import app.partsvibe.storage.domain.StoredFileKind;
import app.partsvibe.storage.domain.StoredFileStatus;
import app.partsvibe.storage.repo.StoredFileRepository;
import app.partsvibe.storage.service.FilesystemStorage;
import app.partsvibe.storage.service.StoragePathResolver;
import app.partsvibe.storage.service.ThumbnailImageService;
import java.io.InputStream;
import org.springframework.stereotype.Component;

@Component
@HandlesEvent(name = FileReadyEvent.EVENT_NAME, version = 1)
class GenerateImageThumbnailsOnFileReadyEventHandler extends BaseEventHandler<FileReadyEvent> {
    private final StoredFileRepository storedFileRepository;
    private final FilesystemStorage filesystemStorage;
    private final StoragePathResolver storagePathResolver;
    private final ThumbnailImageService thumbnailImageService;

    GenerateImageThumbnailsOnFileReadyEventHandler(
            StoredFileRepository storedFileRepository,
            FilesystemStorage filesystemStorage,
            StoragePathResolver storagePathResolver,
            ThumbnailImageService thumbnailImageService) {
        this.storedFileRepository = storedFileRepository;
        this.filesystemStorage = filesystemStorage;
        this.storagePathResolver = storagePathResolver;
        this.thumbnailImageService = thumbnailImageService;
    }

    @Override
    protected void doHandle(FileReadyEvent event) {
        var storedFile = storedFileRepository.findByFileId(event.fileId()).orElse(null);
        if (storedFile == null || storedFile.getStatus() != StoredFileStatus.READY) {
            return;
        }
        if (storedFile.getKind() != StoredFileKind.IMAGE) {
            return;
        }

        byte[] bytes = readAllBytes(storagePathResolver.blobPath(event.fileId()));
        String format = "image/png".equalsIgnoreCase(storedFile.getMimeType()) ? "png" : "jpg";

        byte[] thumb128 = thumbnailImageService.createThumbnail(bytes, 128, format);
        byte[] thumb512 = thumbnailImageService.createThumbnail(bytes, 512, format);
        filesystemStorage.writeThumbnail128(event.fileId(), thumb128);
        filesystemStorage.writeThumbnail512(event.fileId(), thumb512);

        storedFile.setThumbnail128Ready(true);
        storedFile.setThumbnail512Ready(true);
        storedFileRepository.save(storedFile);
    }

    private byte[] readAllBytes(java.nio.file.Path path) {
        try (InputStream input = filesystemStorage.openForRead(path)) {
            return input.readAllBytes();
        } catch (java.io.IOException ex) {
            throw new app.partsvibe.shared.error.ApplicationException(
                    "Failed to read file for thumbnail generation.", ex);
        }
    }
}
