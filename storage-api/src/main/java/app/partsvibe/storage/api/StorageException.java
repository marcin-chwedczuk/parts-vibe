package app.partsvibe.storage.api;

import app.partsvibe.shared.error.ApplicationException;

public class StorageException extends ApplicationException {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
