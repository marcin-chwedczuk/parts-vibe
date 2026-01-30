package app.partsvibe.search.api;

public interface CatalogSearchService {
    String indexText(String text);

    CatalogSearchResult search(String queryText, int page, int pageSize);
}
