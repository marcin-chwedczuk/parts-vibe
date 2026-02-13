package app.partsvibe.search.api;

public interface SearchServiceClient {
    String indexText(String text);

    CatalogSearchResult search(String queryText, int page, int pageSize);
}
