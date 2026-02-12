package app.partsvibe.shared.cqrs;

@FunctionalInterface
public interface CommandExecution<C extends Command<R>, R> {
    R execute(C command);
}
