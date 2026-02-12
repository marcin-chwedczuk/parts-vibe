package app.partsvibe.shared.cqrs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public abstract class BaseCommandHandler<C extends Command<R>, R> implements CommandHandler<C, R> {
    protected static final Logger log = LoggerFactory.getLogger(BaseCommandHandler.class);

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public R handle(C command) {
        log.debug(
                "Command handler invoked. handlerClass={}, commandClass={}",
                getClass().getSimpleName(),
                command.getClass().getSimpleName());
        return doHandle(command);
    }

    protected abstract R doHandle(C command);
}
