package app.partsvibe.shared.cqrs;

public interface CommandBehavior<C extends Command<R>, R> {
    R handle(C command, CommandExecution<C, R> next);
}
