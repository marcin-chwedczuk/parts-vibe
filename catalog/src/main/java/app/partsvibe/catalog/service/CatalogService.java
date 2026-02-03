package app.partsvibe.catalog.service;

import app.partsvibe.search.api.CatalogSearchResult;

public interface CatalogService {
    String indexText(String text);

    CatalogSearchResult search(String query, int page, int pageSize);
}
