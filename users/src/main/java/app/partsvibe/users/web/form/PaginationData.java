package app.partsvibe.users.web.form;

import app.partsvibe.uicomponents.pagination.PaginationModel;
import java.util.List;

public record PaginationData(
        String firstUrl,
        String lastUrl,
        List<PageLink> pageLinks,
        int totalPages,
        int currentPage,
        int startRow,
        int endRow,
        long totalRows)
        implements PaginationModel {}
