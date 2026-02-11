package app.partsvibe.infra.events.handling;

import app.partsvibe.shared.error.ApplicationException;

public class UnknownEventTypeException extends ApplicationException {
    public UnknownEventTypeException(String message) {
        super(message);
    }
}
