package app.partsvibe.storage.api;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.shared.error.ApplicationException;
import app.partsvibe.storage.api.DeleteFileResult.Status;
import app.partsvibe.storage.commands.DeleteFileCommand;
import app.partsvibe.storage.commands.UploadFileCommand;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class DefaultStorageClient implements StorageClient {
    private static final Logger log = LoggerFactory.getLogger(DefaultStorageClient.class);

    private final Mediator mediator;

    DefaultStorageClient(Mediator mediator) {
        this.mediator = mediator;
    }

    @Override
    public StorageUploadResult upload(StorageUploadRequest request) {
        try {
            return mediator.executeCommand(
                    new UploadFileCommand(request.objectType(), request.originalFilename(), request.content()));
        } catch (StorageException ex) {
            throw ex;
        } catch (ApplicationException ex) {
            throw new StorageException("Storage upload failed.", ex);
        }
    }

    @Override
    public DeleteFileResult delete(UUID fileId) {
        try {
            DeleteFileResult result = mediator.executeCommand(new DeleteFileCommand(fileId));
            if (result.status() == Status.FAILED) {
                log.warn("Storage delete returned FAILED status. fileId={}", fileId);
            }
            return result;
        } catch (StorageException ex) {
            log.error("Storage delete failed with storage exception. fileId={}", fileId, ex);
            return DeleteFileResult.failed();
        } catch (ApplicationException ex) {
            log.error("Storage delete failed with application exception. fileId={}", fileId, ex);
            return DeleteFileResult.failed();
        }
    }
}
