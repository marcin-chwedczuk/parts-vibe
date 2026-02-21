package app.partsvibe.storage.errors;

import app.partsvibe.shared.error.ApplicationException;
import java.util.UUID;

public class StoredFileNotFoundException extends ApplicationException {
    public StoredFileNotFoundException(UUID fileId) {
        super("Stored file not found. fileId=" + fileId);
    }
}
