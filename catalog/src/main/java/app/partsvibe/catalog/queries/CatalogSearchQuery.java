package app.partsvibe.catalog.queries;

import app.partsvibe.search.api.CatalogSearchResult;
import app.partsvibe.shared.cqrs.Query;

public record CatalogSearchQuery(String query, int page, int pageSize) implements Query<CatalogSearchResult> {}
