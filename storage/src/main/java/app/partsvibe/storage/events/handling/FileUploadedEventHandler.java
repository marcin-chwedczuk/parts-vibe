package app.partsvibe.storage.events.handling;

import app.partsvibe.shared.antivirus.AntivirusScanner;
import app.partsvibe.shared.antivirus.ScanResult;
import app.partsvibe.shared.events.handling.BaseEventHandler;
import app.partsvibe.shared.events.handling.HandlesEvent;
import app.partsvibe.shared.events.publishing.EventPublisher;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.storage.api.StorageValidationException;
import app.partsvibe.storage.api.events.FileReadyEvent;
import app.partsvibe.storage.api.events.FileUploadedEvent;
import app.partsvibe.storage.domain.StoredFileStatus;
import app.partsvibe.storage.errors.StoredFileNotFoundException;
import app.partsvibe.storage.repo.StoredFileRepository;
import app.partsvibe.storage.service.FileMimeDetector;
import app.partsvibe.storage.service.FilesystemStorage;
import app.partsvibe.storage.service.StoragePathResolver;
import app.partsvibe.storage.service.StorageRules;
import java.io.InputStream;
import org.springframework.stereotype.Component;

@Component
@HandlesEvent(name = FileUploadedEvent.EVENT_NAME, version = 1)
class FileUploadedEventHandler extends BaseEventHandler<FileUploadedEvent> {
    private final StoredFileRepository storedFileRepository;
    private final AntivirusScanner antivirusScanner;
    private final FilesystemStorage filesystemStorage;
    private final StoragePathResolver storagePathResolver;
    private final FileMimeDetector fileMimeDetector;
    private final StorageRules storageRules;
    private final EventPublisher eventPublisher;
    private final TimeProvider timeProvider;

    FileUploadedEventHandler(
            StoredFileRepository storedFileRepository,
            AntivirusScanner antivirusScanner,
            FilesystemStorage filesystemStorage,
            StoragePathResolver storagePathResolver,
            FileMimeDetector fileMimeDetector,
            StorageRules storageRules,
            EventPublisher eventPublisher,
            TimeProvider timeProvider) {
        this.storedFileRepository = storedFileRepository;
        this.antivirusScanner = antivirusScanner;
        this.filesystemStorage = filesystemStorage;
        this.storagePathResolver = storagePathResolver;
        this.fileMimeDetector = fileMimeDetector;
        this.storageRules = storageRules;
        this.eventPublisher = eventPublisher;
        this.timeProvider = timeProvider;
    }

    @Override
    protected void doHandle(FileUploadedEvent event) {
        var storedFile = storedFileRepository
                .findByFileId(event.fileId())
                .orElseThrow(() -> new StoredFileNotFoundException(event.fileId()));

        if (storedFile.getStatus() != StoredFileStatus.PENDING_SCAN) {
            return;
        }

        byte[] bytes = readAllBytes(storagePathResolver.blobPath(event.fileId()));
        ScanResult scanResult = antivirusScanner.scan(new java.io.ByteArrayInputStream(bytes));

        if (scanResult.status() != ScanResult.Status.OK) {
            storedFile.setStatus(StoredFileStatus.REJECTED);
            storedFile.setScannedAt(timeProvider.now());
            storedFileRepository.save(storedFile);
            filesystemStorage.deleteFileDirectory(storedFile.getFileId());
            return;
        }

        String mimeType;
        try {
            mimeType = fileMimeDetector.detect(bytes, storedFile.getOriginalFilename());
            storageRules.validateDetectedMimeType(storedFile.getObjectType(), mimeType);
        } catch (StorageValidationException ex) {
            storedFile.setStatus(StoredFileStatus.REJECTED);
            storedFile.setScannedAt(timeProvider.now());
            storedFileRepository.save(storedFile);
            filesystemStorage.deleteFileDirectory(storedFile.getFileId());
            return;
        }

        storedFile.setMimeType(mimeType);
        storedFile.setStatus(StoredFileStatus.READY);
        storedFile.setScannedAt(timeProvider.now());
        storedFileRepository.save(storedFile);

        FileReadyEvent readyEvent = FileReadyEvent.create(
                storedFile.getFileId(),
                storedFile.getObjectType(),
                event.requestId().orElse(null),
                timeProvider.now());
        eventPublisher.publish(readyEvent);
    }

    private byte[] readAllBytes(java.nio.file.Path path) {
        try (InputStream input = filesystemStorage.openForRead(path)) {
            return input.readAllBytes();
        } catch (java.io.IOException ex) {
            throw new app.partsvibe.shared.error.ApplicationException("Failed to read uploaded file for scanning.", ex);
        }
    }
}
