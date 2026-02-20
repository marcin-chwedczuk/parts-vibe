package app.partsvibe.users.web.form;

import java.util.List;

public record PaginationData(
        String firstUrl,
        String lastUrl,
        List<PageLink> pageLinks,
        int totalPages,
        int currentPage,
        List<Integer> pageNumbers,
        int startRow,
        int endRow,
        long totalRows) {}
