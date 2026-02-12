package app.partsvibe.shared.cqrs;

public interface QueryHandler<Q extends Query<R>, R> {
    R handle(Q query);
}
