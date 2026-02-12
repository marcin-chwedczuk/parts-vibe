package app.partsvibe.shared.cqrs;

@FunctionalInterface
public interface QueryExecution<Q extends Query<R>, R> {
    R execute(Q query);
}
