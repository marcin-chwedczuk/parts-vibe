package app.partsvibe.storage.commands;

import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.storage.api.DeleteFileResult;
import app.partsvibe.storage.api.StorageObjectType;
import app.partsvibe.storage.domain.StoredFileStatus;
import app.partsvibe.storage.repo.StoredFileRepository;
import app.partsvibe.storage.service.FilesystemStorage;
import app.partsvibe.storage.service.StoragePathResolver;
import app.partsvibe.storage.test.it.AbstractStorageIntegrationTest;
import app.partsvibe.storage.test.support.StorageTestData;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DeleteFileCommandHandlerIT extends AbstractStorageIntegrationTest {
    @Autowired
    private DeleteFileCommandHandler commandHandler;

    @Autowired
    private StoredFileRepository storedFileRepository;

    @Autowired
    private StoragePathResolver pathResolver;

    @Autowired
    private FilesystemStorage filesystemStorage;

    @Test
    void deleteMarksFileAsDeletedAndRemovesFilesystemDirectory() {
        UUID fileId = UUID.randomUUID();
        var stored = StorageTestData.pendingImageFile(fileId, StorageObjectType.USER_AVATAR_IMAGE, "avatar.png", 123);
        stored.setStatus(StoredFileStatus.READY);
        stored.setMimeType("image/png");
        stored.setScannedAt(Instant.now());
        stored.setThumbnail128Ready(true);
        stored.setThumbnail512Ready(true);
        storedFileRepository.save(stored);

        filesystemStorage.writeBlob(fileId, StorageTestData.pngBytes(4, 4));
        filesystemStorage.writeThumbnail128(fileId, StorageTestData.pngBytes(2, 2));
        filesystemStorage.writeThumbnail512(fileId, StorageTestData.pngBytes(2, 2));

        DeleteFileResult result = commandHandler.handle(new DeleteFileCommand(fileId));

        assertThat(result.status()).isEqualTo(DeleteFileResult.Status.DELETED);
        var updated = storedFileRepository.findByFileId(fileId).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(StoredFileStatus.DELETED);
        assertThat(updated.getDeletedAt()).isNotNull();
        assertThat(updated.isThumbnail128Ready()).isFalse();
        assertThat(updated.isThumbnail512Ready()).isFalse();
        assertThat(filesystemStorage.exists(pathResolver.fileDirectory(fileId))).isFalse();
    }

    @Test
    void deleteReturnsNotFoundWhenFileDoesNotExist() {
        DeleteFileResult result = commandHandler.handle(new DeleteFileCommand(UUID.randomUUID()));

        assertThat(result.status()).isEqualTo(DeleteFileResult.Status.NOT_FOUND);
    }

    @Test
    void deleteReturnsNotFoundWhenFileAlreadyDeleted() {
        UUID fileId = UUID.randomUUID();
        var stored = StorageTestData.pendingImageFile(fileId, StorageObjectType.USER_AVATAR_IMAGE, "avatar.png", 123);
        stored.setStatus(StoredFileStatus.DELETED);
        stored.setDeletedAt(Instant.now());
        storedFileRepository.save(stored);

        DeleteFileResult result = commandHandler.handle(new DeleteFileCommand(fileId));

        assertThat(result.status()).isEqualTo(DeleteFileResult.Status.NOT_FOUND);
    }
}
