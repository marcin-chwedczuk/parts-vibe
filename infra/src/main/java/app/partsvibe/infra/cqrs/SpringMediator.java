package app.partsvibe.infra.cqrs;

import app.partsvibe.shared.cqrs.Command;
import app.partsvibe.shared.cqrs.CommandBehavior;
import app.partsvibe.shared.cqrs.CommandExecution;
import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.shared.cqrs.Query;
import app.partsvibe.shared.cqrs.QueryBehavior;
import app.partsvibe.shared.cqrs.QueryExecution;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SpringMediator implements Mediator {
    private static final Logger log = LoggerFactory.getLogger(SpringMediator.class);

    private final SpringCqrsHandlerResolver handlerResolver;

    public SpringMediator(SpringCqrsHandlerResolver handlerResolver) {
        this.handlerResolver = handlerResolver;
    }

    @Override
    public <R, C extends Command<R>> R executeCommand(C command) {
        var handler = handlerResolver.resolveCommandHandler(command);
        var behaviors = handlerResolver.resolveCommandBehaviors(command);

        log.debug(
                "Executing command via mediator. commandClass={}, handlerClass={}, behaviorCount={}",
                command.getClass().getSimpleName(),
                handler.getClass().getSimpleName(),
                behaviors.size());

        return buildCommandChain(handler::handle, behaviors).execute(command);
    }

    @Override
    public <R, Q extends Query<R>> R executeQuery(Q query) {
        var handler = handlerResolver.resolveQueryHandler(query);
        var behaviors = handlerResolver.resolveQueryBehaviors(query);

        log.debug(
                "Executing query via mediator. queryClass={}, handlerClass={}, behaviorCount={}",
                query.getClass().getSimpleName(),
                handler.getClass().getSimpleName(),
                behaviors.size());

        return buildQueryChain(handler::handle, behaviors).execute(query);
    }

    private <C extends Command<R>, R> CommandExecution<C, R> buildCommandChain(
            CommandExecution<C, R> handlerExecution, List<CommandBehavior<C, R>> behaviors) {
        var chain = handlerExecution;
        for (var i = behaviors.size() - 1; i >= 0; i--) {
            var behavior = behaviors.get(i);
            var next = chain;
            chain = command -> behavior.handle(command, next);
        }
        return chain;
    }

    private <Q extends Query<R>, R> QueryExecution<Q, R> buildQueryChain(
            QueryExecution<Q, R> handlerExecution, List<QueryBehavior<Q, R>> behaviors) {
        var chain = handlerExecution;
        for (var i = behaviors.size() - 1; i >= 0; i--) {
            var behavior = behaviors.get(i);
            var next = chain;
            chain = query -> behavior.handle(query, next);
        }
        return chain;
    }
}
