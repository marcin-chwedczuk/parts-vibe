package app.partsvibe.catalog.service.impl;

import app.partsvibe.catalog.service.CatalogService;
import app.partsvibe.search.api.CatalogSearchResult;
import app.partsvibe.search.api.CatalogSearchService;
import org.springframework.stereotype.Service;

@Service
public class CatalogServiceImpl implements CatalogService {
    private final CatalogSearchService catalogSearchService;

    public CatalogServiceImpl(CatalogSearchService catalogSearchService) {
        this.catalogSearchService = catalogSearchService;
    }

    @Override
    public String indexText(String text) {
        return catalogSearchService.indexText(text);
    }

    @Override
    public CatalogSearchResult search(String query, int page, int pageSize) {
        return catalogSearchService.search(query, page, pageSize);
    }
}
