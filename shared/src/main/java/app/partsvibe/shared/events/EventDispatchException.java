package app.partsvibe.shared.events;

import app.partsvibe.shared.error.ApplicationException;

public class EventDispatchException extends ApplicationException {
    public EventDispatchException(String message) {
        super(message);
    }

    public EventDispatchException(String message, Throwable cause) {
        super(message, cause);
    }
}
