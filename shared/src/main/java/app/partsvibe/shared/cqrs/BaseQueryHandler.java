package app.partsvibe.shared.cqrs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public abstract class BaseQueryHandler<Q extends Query<R>, R> implements QueryHandler<Q, R> {
    protected static final Logger log = LoggerFactory.getLogger(BaseQueryHandler.class);

    @Override
    @Transactional(propagation = Propagation.REQUIRED, readOnly = true)
    public R handle(Q query) {
        log.debug(
                "Query handler invoked. handlerClass={}, queryClass={}",
                getClass().getSimpleName(),
                query.getClass().getSimpleName());
        return doHandle(query);
    }

    protected abstract R doHandle(Q query);
}
