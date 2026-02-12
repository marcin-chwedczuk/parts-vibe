package app.partsvibe.shared.cqrs;

public interface QueryBehavior<Q extends Query<R>, R> {
    R handle(Q query, QueryExecution<Q, R> next);
}
