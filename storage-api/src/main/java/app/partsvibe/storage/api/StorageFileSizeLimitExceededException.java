package app.partsvibe.storage.api;

public class StorageFileSizeLimitExceededException extends StorageValidationException {
    private final StorageObjectType objectType;
    private final long maxAllowedBytes;
    private final long actualBytes;

    public StorageFileSizeLimitExceededException(StorageObjectType objectType, long maxAllowedBytes, long actualBytes) {
        super("File size exceeds limit. objectType=" + objectType + ", maxBytes=" + maxAllowedBytes + ", actualBytes="
                + actualBytes);
        this.objectType = objectType;
        this.maxAllowedBytes = maxAllowedBytes;
        this.actualBytes = actualBytes;
    }

    public StorageObjectType objectType() {
        return objectType;
    }

    public long maxAllowedBytes() {
        return maxAllowedBytes;
    }

    public long actualBytes() {
        return actualBytes;
    }
}
