package app.partsvibe.shared.cqrs;

public interface PaginatedQuery<R> extends Query<R> {
    int currentPage();

    int pageSize();
}
