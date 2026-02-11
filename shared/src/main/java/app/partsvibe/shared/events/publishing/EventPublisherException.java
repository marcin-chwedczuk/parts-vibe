package app.partsvibe.shared.events.publishing;

import app.partsvibe.shared.error.ApplicationException;

public class EventPublisherException extends ApplicationException {
    public EventPublisherException(String message) {
        super(message);
    }

    public EventPublisherException(String message, Throwable cause) {
        super(message, cause);
    }
}
