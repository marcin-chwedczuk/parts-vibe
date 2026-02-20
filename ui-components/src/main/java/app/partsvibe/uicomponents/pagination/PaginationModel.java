package app.partsvibe.uicomponents.pagination;

import java.util.List;

public interface PaginationModel {
    String firstUrl();

    String lastUrl();

    List<? extends PaginationLinkModel> pageLinks();

    int totalPages();

    int currentPage();

    int startRow();

    int endRow();

    long totalRows();
}
