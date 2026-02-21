package app.partsvibe.catalog.errors;

import app.partsvibe.shared.error.ApplicationException;

public class PartNotFoundException extends ApplicationException {
    public PartNotFoundException(Long partId) {
        super("Part not found. partId=" + partId);
    }
}
