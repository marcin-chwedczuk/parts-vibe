package app.partsvibe.shared.events;

import app.partsvibe.shared.error.ApplicationException;

public class UnknownEventTypeException extends ApplicationException {
    public UnknownEventTypeException(String message) {
        super(message);
    }
}
