package app.partsvibe.catalog.service.impl;

import app.partsvibe.catalog.service.CatalogService;
import app.partsvibe.search.api.CatalogSearchResult;
import app.partsvibe.search.api.SearchServiceClient;
import org.springframework.stereotype.Service;

@Service
public class CatalogServiceImpl implements CatalogService {
    private final SearchServiceClient searchServiceClient;

    public CatalogServiceImpl(SearchServiceClient searchServiceClient) {
        this.searchServiceClient = searchServiceClient;
    }

    @Override
    public String indexText(String text) {
        return searchServiceClient.indexText(text);
    }

    @Override
    public CatalogSearchResult search(String query, int page, int pageSize) {
        return searchServiceClient.search(query, page, pageSize);
    }
}
