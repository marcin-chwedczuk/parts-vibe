package app.partsvibe.shared.cqrs;

public interface Mediator {
    <R, C extends Command<R>> R executeCommand(C command);

    <R, Q extends Query<R>> R executeQuery(Q query);
}
