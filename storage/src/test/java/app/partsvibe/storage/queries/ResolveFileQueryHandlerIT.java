package app.partsvibe.storage.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.storage.api.StorageFileVariant;
import app.partsvibe.storage.api.StorageObjectType;
import app.partsvibe.storage.domain.StoredFileStatus;
import app.partsvibe.storage.errors.StoredFileNotFoundException;
import app.partsvibe.storage.repo.StoredFileRepository;
import app.partsvibe.storage.service.FilesystemStorage;
import app.partsvibe.storage.service.StoragePathResolver;
import app.partsvibe.storage.test.it.AbstractStorageIntegrationTest;
import app.partsvibe.storage.test.support.StorageTestData;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ResolveFileQueryHandlerIT extends AbstractStorageIntegrationTest {
    @Autowired
    private ResolveFileQueryHandler queryHandler;

    @Autowired
    private StoredFileRepository storedFileRepository;

    @Autowired
    private FilesystemStorage filesystemStorage;

    @Autowired
    private StoragePathResolver pathResolver;

    @Test
    void resolvesThumbnailWhenReadyAndPresent() {
        UUID fileId = UUID.randomUUID();
        var stored = StorageTestData.pendingImageFile(fileId, StorageObjectType.USER_AVATAR_IMAGE, "image.png", 100);
        stored.setStatus(StoredFileStatus.READY);
        stored.setMimeType("image/png");
        stored.setScannedAt(Instant.now());
        stored.setThumbnail128Ready(true);
        storedFileRepository.save(stored);

        filesystemStorage.writeBlob(fileId, StorageTestData.pngBytes(10, 10));
        byte[] thumb128 = StorageTestData.pngBytes(2, 2);
        filesystemStorage.writeThumbnail128(fileId, thumb128);

        var result = queryHandler.handle(new ResolveFileQuery(fileId, StorageFileVariant.THUMBNAIL_128));

        assertThat(result.path()).isEqualTo(pathResolver.thumbnail128Path(fileId));
        assertThat(result.mimeType()).isEqualTo("image/png");
        assertThat(result.sizeBytes()).isEqualTo(thumb128.length);
    }

    @Test
    void fallsBackToOriginalWhenThumbnailRequestedButNotAvailable() {
        UUID fileId = UUID.randomUUID();
        var stored = StorageTestData.pendingImageFile(fileId, StorageObjectType.USER_AVATAR_IMAGE, "image.png", 100);
        stored.setStatus(StoredFileStatus.READY);
        stored.setMimeType("image/png");
        stored.setScannedAt(Instant.now());
        stored.setThumbnail128Ready(true);
        storedFileRepository.save(stored);

        byte[] original = StorageTestData.pngBytes(10, 10);
        filesystemStorage.writeBlob(fileId, original);

        var result = queryHandler.handle(new ResolveFileQuery(fileId, StorageFileVariant.THUMBNAIL_128));

        assertThat(result.path()).isEqualTo(pathResolver.blobPath(fileId));
        assertThat(result.sizeBytes()).isEqualTo(original.length);
    }

    @Test
    void throwsWhenFileIsNotReady() {
        UUID fileId = UUID.randomUUID();
        var stored = StorageTestData.pendingImageFile(fileId, StorageObjectType.USER_AVATAR_IMAGE, "image.png", 100);
        stored.setStatus(StoredFileStatus.PENDING_SCAN);
        storedFileRepository.save(stored);

        assertThatThrownBy(() -> queryHandler.handle(new ResolveFileQuery(fileId, StorageFileVariant.ORIGINAL)))
                .isInstanceOf(StoredFileNotFoundException.class);
    }

    @Test
    void resolvesWithOctetStreamWhenMimeTypeIsMissing() {
        UUID fileId = UUID.randomUUID();
        var stored = StorageTestData.pendingImageFile(fileId, StorageObjectType.USER_AVATAR_IMAGE, "image.png", 100);
        stored.setStatus(StoredFileStatus.READY);
        stored.setMimeType(null);
        stored.setScannedAt(Instant.now());
        storedFileRepository.save(stored);

        byte[] original = StorageTestData.pngBytes(10, 10);
        filesystemStorage.writeBlob(fileId, original);

        var result = queryHandler.handle(new ResolveFileQuery(fileId, StorageFileVariant.ORIGINAL));

        assertThat(result.mimeType()).isEqualTo("application/octet-stream");
    }

    @Test
    void throwsWhenNeitherThumbnailNorOriginalExists() {
        UUID fileId = UUID.randomUUID();
        var stored = StorageTestData.pendingImageFile(fileId, StorageObjectType.USER_AVATAR_IMAGE, "image.png", 100);
        stored.setStatus(StoredFileStatus.READY);
        stored.setMimeType("image/png");
        stored.setScannedAt(Instant.now());
        stored.setThumbnail128Ready(true);
        storedFileRepository.save(stored);

        assertThatThrownBy(() -> queryHandler.handle(new ResolveFileQuery(fileId, StorageFileVariant.THUMBNAIL_128)))
                .isInstanceOf(StoredFileNotFoundException.class);
    }

    @Test
    void resolvesThumbnail512WhenReadyAndPresent() {
        UUID fileId = UUID.randomUUID();
        var stored = StorageTestData.pendingImageFile(fileId, StorageObjectType.USER_AVATAR_IMAGE, "image.png", 100);
        stored.setStatus(StoredFileStatus.READY);
        stored.setMimeType("image/png");
        stored.setScannedAt(Instant.now());
        stored.setThumbnail512Ready(true);
        storedFileRepository.save(stored);

        byte[] original = StorageTestData.pngBytes(10, 10);
        byte[] thumb512 = StorageTestData.pngBytes(5, 5);
        filesystemStorage.writeBlob(fileId, original);
        filesystemStorage.writeThumbnail512(fileId, thumb512);

        var result = queryHandler.handle(new ResolveFileQuery(fileId, StorageFileVariant.THUMBNAIL_512));

        assertThat(result.path()).isEqualTo(pathResolver.thumbnail512Path(fileId));
        assertThat(result.sizeBytes()).isEqualTo(thumb512.length);
    }
}
