package app.partsvibe.search.queries;

import app.partsvibe.search.api.CatalogSearchResult;
import app.partsvibe.shared.cqrs.Query;

public record SearchCatalogQuery(String queryText, int page, int pageSize) implements Query<CatalogSearchResult> {}
