package app.partsvibe.catalog.queries;

import app.partsvibe.shared.cqrs.Query;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record PartByIdQuery(@Positive Long partId) implements Query<PartByIdQuery.PartDetails> {
    public record PartDetails(
            Long id, String name, String description, Long categoryId, String categoryName, List<TagCard> tags) {}

    public record TagCard(String name, String color) {}
}
