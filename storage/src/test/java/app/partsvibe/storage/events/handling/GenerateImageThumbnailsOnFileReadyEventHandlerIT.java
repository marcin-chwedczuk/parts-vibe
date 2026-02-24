package app.partsvibe.storage.events.handling;

import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.storage.api.StorageObjectType;
import app.partsvibe.storage.api.events.FileReadyEvent;
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

class GenerateImageThumbnailsOnFileReadyEventHandlerIT extends AbstractStorageIntegrationTest {
    @Autowired
    private GenerateImageThumbnailsOnFileReadyEventHandler handler;

    @Autowired
    private StoredFileRepository storedFileRepository;

    @Autowired
    private FilesystemStorage filesystemStorage;

    @Autowired
    private StoragePathResolver pathResolver;

    @Test
    void createsThumbnailsAndMarksFlagsForReadyImageFile() {
        UUID fileId = UUID.randomUUID();
        byte[] png = StorageTestData.pngBytes(30, 10);

        var stored =
                StorageTestData.pendingImageFile(fileId, StorageObjectType.USER_AVATAR_IMAGE, "ok.png", png.length);
        stored.setStatus(StoredFileStatus.READY);
        stored.setMimeType("image/png");
        stored.setScannedAt(Instant.now());
        storedFileRepository.save(stored);
        filesystemStorage.writeBlob(fileId, png);

        handler.handle(FileReadyEvent.create(fileId, StorageObjectType.USER_AVATAR_IMAGE));

        var saved = storedFileRepository.findByFileId(fileId).orElseThrow();
        assertThat(saved.isThumbnail128Ready()).isTrue();
        assertThat(saved.isThumbnail512Ready()).isTrue();
        assertThat(filesystemStorage.exists(pathResolver.thumbnail128Path(fileId)))
                .isTrue();
        assertThat(filesystemStorage.exists(pathResolver.thumbnail512Path(fileId)))
                .isTrue();
    }

    @Test
    void skipsNonImageFiles() {
        UUID fileId = UUID.randomUUID();
        byte[] payload = "pdf-bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        var stored =
                StorageTestData.pendingBlobFile(fileId, StorageObjectType.PART_ATTACHMENT, "doc.pdf", payload.length);
        stored.setStatus(StoredFileStatus.READY);
        stored.setMimeType("application/pdf");
        stored.setScannedAt(Instant.now());
        storedFileRepository.save(stored);
        filesystemStorage.writeBlob(fileId, payload);

        handler.handle(FileReadyEvent.create(fileId, StorageObjectType.PART_ATTACHMENT));

        var saved = storedFileRepository.findByFileId(fileId).orElseThrow();
        assertThat(saved.isThumbnail128Ready()).isFalse();
        assertThat(saved.isThumbnail512Ready()).isFalse();
        assertThat(filesystemStorage.exists(pathResolver.thumbnail128Path(fileId)))
                .isFalse();
        assertThat(filesystemStorage.exists(pathResolver.thumbnail512Path(fileId)))
                .isFalse();
    }
}
