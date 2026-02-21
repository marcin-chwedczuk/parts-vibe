package app.partsvibe.storage.errors;

import app.partsvibe.shared.error.ApplicationException;

public class StorageValidationException extends ApplicationException {
    public StorageValidationException(String message) {
        super(message);
    }
}
