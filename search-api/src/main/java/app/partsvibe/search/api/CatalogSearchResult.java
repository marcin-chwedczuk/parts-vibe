package app.partsvibe.search.api;

import java.util.List;

public record CatalogSearchResult(List<CatalogSearchHit> hits, long total, int page, int pageSize) {
  public boolean hasNext() {
    return (long) (page + 1) * pageSize < total;
  }

  public boolean hasPrev() {
    return page > 0;
  }

  public int nextPage() {
    return page + 1;
  }

  public int prevPage() {
    return page - 1;
  }
}
