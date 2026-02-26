package app.partsvibe.storage.events.handling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.shared.antivirus.ScanResult;
import app.partsvibe.storage.api.StorageObjectType;
import app.partsvibe.storage.api.events.FileReadyEvent;
import app.partsvibe.storage.api.events.FileUploadedEvent;
import app.partsvibe.storage.domain.StoredFileStatus;
import app.partsvibe.storage.errors.StoredFileNotFoundException;
import app.partsvibe.storage.repo.StoredFileRepository;
import app.partsvibe.storage.service.FilesystemStorage;
import app.partsvibe.storage.service.StoragePathResolver;
import app.partsvibe.storage.test.it.AbstractStorageIntegrationTest;
import app.partsvibe.storage.test.support.StorageTestData;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ScanAndValidateUploadedFileOnFileUploadedEventHandlerIT extends AbstractStorageIntegrationTest {
    @Autowired
    private ScanAndValidateUploadedFileOnFileUploadedEventHandler handler;

    @Autowired
    private StoredFileRepository storedFileRepository;

    @Autowired
    private FilesystemStorage filesystemStorage;

    @Autowired
    private StoragePathResolver pathResolver;

    @Test
    void marksFileReadyAndPublishesFileReadyEventWhenScanAndMimeValidationPass() {
        UUID fileId = UUID.randomUUID();
        byte[] png = StorageTestData.pngBytes(8, 8);
        storedFileRepository.save(
                StorageTestData.pendingImageFile(fileId, StorageObjectType.USER_AVATAR_IMAGE, "ok.png", png.length));
        filesystemStorage.writeBlob(fileId, png);

        handler.handle(FileUploadedEvent.builder()
                .fileId(fileId)
                .objectType(StorageObjectType.USER_AVATAR_IMAGE)
                .build());

        var saved = storedFileRepository.findByFileId(fileId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(StoredFileStatus.READY);
        assertThat(saved.getScannedAt()).isNotNull();
        assertThat(saved.getMimeType()).isEqualTo("image/png");

        assertThat(eventPublisher.publishedEvents()).hasSize(1);
        assertThat(eventPublisher.publishedEvents().getFirst()).isInstanceOf(FileReadyEvent.class);
        FileReadyEvent readyEvent =
                (FileReadyEvent) eventPublisher.publishedEvents().getFirst();
        assertThat(readyEvent.fileId()).isEqualTo(fileId);
    }

    @Test
    void rejectsFileAndDeletesDirectoryWhenAntivirusReportsMalware() {
        UUID fileId = UUID.randomUUID();
        byte[] png = StorageTestData.pngBytes(8, 8);
        storedFileRepository.save(
                StorageTestData.pendingImageFile(fileId, StorageObjectType.USER_AVATAR_IMAGE, "bad.png", png.length));
        filesystemStorage.writeBlob(fileId, png);
        fakeAntivirusScanner.setNextResult(
                new ScanResult(ScanResult.Status.MALWARE_FOUND, Optional.of("Eicar-Test-Signature")));

        handler.handle(FileUploadedEvent.builder()
                .fileId(fileId)
                .objectType(StorageObjectType.USER_AVATAR_IMAGE)
                .build());

        var saved = storedFileRepository.findByFileId(fileId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(StoredFileStatus.REJECTED);
        assertThat(saved.getScannedAt()).isNotNull();
        assertThat(filesystemStorage.exists(pathResolver.fileDirectory(fileId))).isFalse();
        assertThat(eventPublisher.publishedEvents()).isEmpty();
    }

    @Test
    void rejectsFileWhenMimeValidationFailsAfterSuccessfulScan() {
        UUID fileId = UUID.randomUUID();
        byte[] text = "plain-text-not-an-image".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        storedFileRepository.save(StorageTestData.pendingImageFile(
                fileId, StorageObjectType.USER_AVATAR_IMAGE, "avatar.png", text.length));
        filesystemStorage.writeBlob(fileId, text);

        handler.handle(FileUploadedEvent.builder()
                .fileId(fileId)
                .objectType(StorageObjectType.USER_AVATAR_IMAGE)
                .build());

        var saved = storedFileRepository.findByFileId(fileId).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(StoredFileStatus.REJECTED);
        assertThat(saved.getScannedAt()).isNotNull();
        assertThat(filesystemStorage.exists(pathResolver.fileDirectory(fileId))).isFalse();
        assertThat(eventPublisher.publishedEvents()).isEmpty();
    }

    @Test
    void skipsProcessingWhenFileIsNotPendingScan() {
        UUID fileId = UUID.randomUUID();
        byte[] png = StorageTestData.pngBytes(8, 8);
        var stored =
                StorageTestData.pendingImageFile(fileId, StorageObjectType.USER_AVATAR_IMAGE, "ok.png", png.length);
        stored.setStatus(StoredFileStatus.READY);
        stored.setScannedAt(Instant.now());
        storedFileRepository.save(stored);
        filesystemStorage.writeBlob(fileId, png);

        handler.handle(FileUploadedEvent.builder()
                .fileId(fileId)
                .objectType(StorageObjectType.USER_AVATAR_IMAGE)
                .build());

        var unchanged = storedFileRepository.findByFileId(fileId).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(StoredFileStatus.READY);
        assertThat(eventPublisher.publishedEvents()).isEmpty();
    }

    @Test
    void throwsWhenStoredFileDoesNotExist() {
        UUID fileId = UUID.randomUUID();

        assertThatThrownBy(() -> handler.handle(FileUploadedEvent.builder()
                        .fileId(fileId)
                        .objectType(StorageObjectType.USER_AVATAR_IMAGE)
                        .build()))
                .isInstanceOf(StoredFileNotFoundException.class);
    }
}
