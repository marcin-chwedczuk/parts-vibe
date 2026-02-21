package app.partsvibe.storage.api;

import java.util.UUID;

public interface StorageClient {
    StorageUploadResult upload(StorageUploadRequest request);

    void delete(UUID fileId);
}
