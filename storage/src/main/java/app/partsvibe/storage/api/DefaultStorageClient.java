package app.partsvibe.storage.api;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.storage.commands.DeleteStoredFileCommand;
import app.partsvibe.storage.commands.UploadStoredFileCommand;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class DefaultStorageClient implements StorageClient {
    private final Mediator mediator;

    DefaultStorageClient(Mediator mediator) {
        this.mediator = mediator;
    }

    @Override
    public StorageUploadResult upload(StorageUploadRequest request) {
        return mediator.executeCommand(
                new UploadStoredFileCommand(request.objectType(), request.originalFilename(), request.content()));
    }

    @Override
    public void delete(UUID fileId) {
        mediator.executeCommand(new DeleteStoredFileCommand(fileId));
    }
}
