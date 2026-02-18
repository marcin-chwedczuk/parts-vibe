package app.partsvibe.shared.cqrs;

public abstract class BasePaginatedQueryHandler<Q extends PaginatedQuery<R>, R> extends BaseQueryHandler<Q, R> {
    protected int resolvePageSize(Q query) {
        if (query.pageSize() <= 0) {
            return PaginationPolicy.DEFAULT_PAGE_SIZE;
        }
        return Math.min(query.pageSize(), PaginationPolicy.MAX_PAGE_SIZE);
    }

    protected int computeTotalPages(long totalRows, int pageSize) {
        return Math.max(1, (int) Math.ceil(totalRows / (double) pageSize));
    }

    protected int resolvePageNumber(Q query, int totalPages) {
        return Math.min(Math.max(1, query.currentPage()), totalPages);
    }
}
