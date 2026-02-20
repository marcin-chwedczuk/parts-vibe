package app.partsvibe.catalog.queries;

import app.partsvibe.shared.cqrs.Query;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record ListCategoryPartsQuery(@Positive Long categoryId, String searchText)
        implements Query<ListCategoryPartsQuery.CategoryParts> {
    public record CategoryParts(Long categoryId, String categoryName, List<PartRow> parts) {}

    public record PartRow(Long id, String name, String descriptionSnippet, List<TagCard> tags) {}

    public record TagCard(String name, String color) {}
}
