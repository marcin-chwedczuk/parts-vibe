package app.partsvibe.users.testsupport;

import app.partsvibe.shared.cqrs.Command;
import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.shared.cqrs.Query;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class InMemoryMediator implements Mediator {
    private final Map<Class<?>, Function<Command<?>, ?>> commandHandlers = new HashMap<>();
    private final Map<Class<?>, Function<Query<?>, ?>> queryHandlers = new HashMap<>();

    public void clear() {
        commandHandlers.clear();
        queryHandlers.clear();
    }

    public <R, C extends Command<R>> void onCommand(Class<C> commandType, Function<C, R> handler) {
        commandHandlers.put(commandType, command -> handler.apply(commandType.cast(command)));
    }

    public <R, Q extends Query<R>> void onQuery(Class<Q> queryType, Function<Q, R> handler) {
        queryHandlers.put(queryType, query -> handler.apply(queryType.cast(query)));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, C extends Command<R>> R executeCommand(C command) {
        Function<Command<?>, ?> handler = commandHandlers.get(command.getClass());
        if (handler == null) {
            throw new IllegalStateException("No test handler registered for command: "
                    + command.getClass().getName());
        }
        return (R) handler.apply(command);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R, Q extends Query<R>> R executeQuery(Q query) {
        Function<Query<?>, ?> handler = queryHandlers.get(query.getClass());
        if (handler == null) {
            throw new IllegalStateException(
                    "No test handler registered for query: " + query.getClass().getName());
        }
        return (R) handler.apply(query);
    }
}
