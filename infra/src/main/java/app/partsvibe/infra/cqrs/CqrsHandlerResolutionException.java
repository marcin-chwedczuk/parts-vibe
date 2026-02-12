package app.partsvibe.infra.cqrs;

import app.partsvibe.shared.error.ApplicationException;

public class CqrsHandlerResolutionException extends ApplicationException {
    public CqrsHandlerResolutionException(String message) {
        super(message);
    }
}
