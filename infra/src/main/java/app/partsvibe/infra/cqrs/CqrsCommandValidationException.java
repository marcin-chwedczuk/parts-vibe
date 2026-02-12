package app.partsvibe.infra.cqrs;

import app.partsvibe.shared.error.ApplicationException;
import java.util.List;

public class CqrsCommandValidationException extends ApplicationException {
    private final List<CqrsValidationError> errors;

    public CqrsCommandValidationException(String commandClass, List<CqrsValidationError> errors) {
        super("Command validation failed. commandClass=%s, errors=%s".formatted(commandClass, errors));
        this.errors = List.copyOf(errors);
    }

    public List<CqrsValidationError> errors() {
        return errors;
    }
}
