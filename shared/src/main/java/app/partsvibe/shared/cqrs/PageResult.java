package app.partsvibe.shared.cqrs;

import java.util.List;

public record PageResult<T>(List<T> items, long totalRows, int totalPages, int currentPage, int pageSize) {
    public boolean isEmpty() {
        return totalRows == 0;
    }

    public int startRow() {
        return isEmpty() ? 0 : ((currentPage - 1) * pageSize) + 1;
    }

    public int endRow() {
        return isEmpty() ? 0 : startRow() + items.size() - 1;
    }
}
