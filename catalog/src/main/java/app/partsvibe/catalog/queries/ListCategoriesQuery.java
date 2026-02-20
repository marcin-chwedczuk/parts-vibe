package app.partsvibe.catalog.queries;

import app.partsvibe.shared.cqrs.Query;
import java.util.List;

public record ListCategoriesQuery() implements Query<List<ListCategoriesQuery.CategoryCard>> {
    public record CategoryCard(Long id, String name, long partsCount, List<TagCard> tags) {}

    public record TagCard(String name, String color) {}
}
