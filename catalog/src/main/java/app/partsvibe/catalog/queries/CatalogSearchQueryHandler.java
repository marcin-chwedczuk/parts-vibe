package app.partsvibe.catalog.queries;

import app.partsvibe.search.api.CatalogSearchResult;
import app.partsvibe.search.api.SearchServiceClient;
import app.partsvibe.shared.cqrs.BaseQueryHandler;
import org.springframework.stereotype.Component;

@Component
class CatalogSearchQueryHandler extends BaseQueryHandler<CatalogSearchQuery, CatalogSearchResult> {
    private final SearchServiceClient searchServiceClient;

    CatalogSearchQueryHandler(SearchServiceClient searchServiceClient) {
        this.searchServiceClient = searchServiceClient;
    }

    @Override
    protected CatalogSearchResult doHandle(CatalogSearchQuery query) {
        return searchServiceClient.search(query.query(), query.page(), query.pageSize());
    }
}
