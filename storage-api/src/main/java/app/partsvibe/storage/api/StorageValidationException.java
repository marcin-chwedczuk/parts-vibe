package app.partsvibe.storage.api;

public class StorageValidationException extends StorageException {
    public StorageValidationException(String message) {
        super(message);
    }

    public StorageValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}
