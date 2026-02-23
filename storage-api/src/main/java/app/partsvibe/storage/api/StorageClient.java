package app.partsvibe.storage.api;

import java.util.UUID;

public interface StorageClient {
    StorageUploadResult upload(StorageUploadRequest request);

    DeleteFileResult delete(UUID fileId);
}
