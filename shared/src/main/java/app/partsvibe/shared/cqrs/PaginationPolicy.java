package app.partsvibe.shared.cqrs;

import java.util.List;

public final class PaginationPolicy {
    public static final int DEFAULT_PAGE_SIZE = 10;
    public static final int MAX_PAGE_SIZE = 50;
    public static final List<Integer> ALLOWED_PAGE_SIZES = List.of(10, 25, 50);

    private PaginationPolicy() {}
}
