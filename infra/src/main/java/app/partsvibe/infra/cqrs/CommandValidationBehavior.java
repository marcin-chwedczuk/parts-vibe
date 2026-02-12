package app.partsvibe.infra.cqrs;

import app.partsvibe.shared.cqrs.Command;
import app.partsvibe.shared.cqrs.CommandBehavior;
import app.partsvibe.shared.cqrs.CommandExecution;
import jakarta.validation.Validator;
import java.util.Comparator;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class CommandValidationBehavior<C extends Command<R>, R> implements CommandBehavior<C, R> {
    private final Validator validator;

    CommandValidationBehavior(Validator validator) {
        this.validator = validator;
    }

    @Override
    public R handle(C command, CommandExecution<C, R> next) {
        var violations = validator.validate(command);
        if (!violations.isEmpty()) {
            var errors = violations.stream()
                    .map(violation ->
                            new CqrsValidationError(violation.getPropertyPath().toString(), violation.getMessage()))
                    .sorted(Comparator.comparing(CqrsValidationError::field)
                            .thenComparing(CqrsValidationError::message))
                    .toList();
            throw new CqrsCommandValidationException(command.getClass().getName(), errors);
        }
        return next.execute(command);
    }
}
