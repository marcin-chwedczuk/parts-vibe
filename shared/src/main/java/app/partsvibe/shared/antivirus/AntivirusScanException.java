package app.partsvibe.shared.antivirus;

import app.partsvibe.shared.error.ApplicationException;

public class AntivirusScanException extends ApplicationException {
    public AntivirusScanException(String message) {
        super(message);
    }

    public AntivirusScanException(String message, Throwable cause) {
        super(message, cause);
    }
}
