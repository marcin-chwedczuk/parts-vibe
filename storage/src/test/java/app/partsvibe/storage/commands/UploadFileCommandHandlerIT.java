package app.partsvibe.storage.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.storage.api.StorageObjectType;
import app.partsvibe.storage.api.StorageValidationException;
import app.partsvibe.storage.api.events.FileUploadedEvent;
import app.partsvibe.storage.domain.StoredFileStatus;
import app.partsvibe.storage.repo.StoredFileRepository;
import app.partsvibe.storage.service.FilesystemStorage;
import app.partsvibe.storage.service.StoragePathResolver;
import app.partsvibe.storage.test.it.AbstractStorageIntegrationTest;
import app.partsvibe.storage.test.support.StorageTestData;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UploadFileCommandHandlerIT extends AbstractStorageIntegrationTest {
    @Autowired
    private UploadFileCommandHandler commandHandler;

    @Autowired
    private StoredFileRepository storedFileRepository;

    @Autowired
    private StoragePathResolver pathResolver;

    @Autowired
    private FilesystemStorage filesystemStorage;

    @Test
    void uploadStoresPendingFileWritesBlobAndPublishesFileUploadedEvent() throws Exception {
        byte[] content = StorageTestData.pngBytes(8, 8);

        var result = commandHandler.handle(UploadFileCommand.builder()
                .objectType(StorageObjectType.USER_AVATAR_IMAGE)
                .originalFilename("avatar.png")
                .content(content)
                .build());

        var saved = storedFileRepository.findByFileId(result.fileId()).orElseThrow();
        assertThat(saved.getStatus()).isEqualTo(StoredFileStatus.PENDING_SCAN);
        assertThat(saved.getMimeType()).isNull();
        assertThat(saved.getSizeBytes()).isEqualTo(content.length);
        assertThat(saved.getOriginalFilename()).isEqualTo("avatar.png");
        assertThat(saved.getUploadedBy())
                .isEqualTo(currentUserProvider.currentUsername().orElseThrow());

        var blobPath = pathResolver.blobPath(result.fileId());
        assertThat(filesystemStorage.exists(blobPath)).isTrue();
        assertThat(Files.readAllBytes(blobPath)).isEqualTo(content);

        assertThat(eventPublisher.publishedEvents()).hasSize(1);
        assertThat(eventPublisher.publishedEvents().getFirst()).isInstanceOf(FileUploadedEvent.class);
        FileUploadedEvent published =
                (FileUploadedEvent) eventPublisher.publishedEvents().getFirst();
        assertThat(published.fileId()).isEqualTo(result.fileId());
        assertThat(published.objectType()).isEqualTo(StorageObjectType.USER_AVATAR_IMAGE);
    }

    @Test
    void uploadRejectsImageWithInvalidExtension() {
        byte[] content = StorageTestData.pngBytes(8, 8);

        assertThatThrownBy(() -> commandHandler.handle(UploadFileCommand.builder()
                        .objectType(StorageObjectType.USER_AVATAR_IMAGE)
                        .originalFilename("avatar.gif")
                        .content(content)
                        .build()))
                .isInstanceOf(StorageValidationException.class);

        assertThat(storedFileRepository.count()).isZero();
        assertThat(eventPublisher.publishedEvents()).isEmpty();
    }
}
