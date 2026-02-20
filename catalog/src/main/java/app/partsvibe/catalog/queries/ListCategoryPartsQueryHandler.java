package app.partsvibe.catalog.queries;

import app.partsvibe.catalog.domain.Part;
import app.partsvibe.catalog.domain.QCategory;
import app.partsvibe.catalog.domain.QPart;
import app.partsvibe.catalog.domain.QTag;
import app.partsvibe.catalog.errors.CategoryNotFoundException;
import app.partsvibe.shared.cqrs.BaseQueryHandler;
import app.partsvibe.shared.utils.StringUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
class ListCategoryPartsQueryHandler
        extends BaseQueryHandler<ListCategoryPartsQuery, ListCategoryPartsQuery.CategoryParts> {
    private static final int DESCRIPTION_SNIPPET_LENGTH = 280;

    private final JPAQueryFactory queryFactory;

    ListCategoryPartsQueryHandler(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    protected ListCategoryPartsQuery.CategoryParts doHandle(ListCategoryPartsQuery query) {
        QCategory category = QCategory.category;
        QPart part = QPart.part;
        QTag tag = QTag.tag;

        String categoryName = queryFactory
                .select(category.name)
                .from(category)
                .where(category.id.eq(query.categoryId()))
                .fetchOne();
        if (categoryName == null) {
            throw new CategoryNotFoundException(query.categoryId());
        }

        BooleanBuilder predicate = new BooleanBuilder().and(part.category.id.eq(query.categoryId()));
        if (StringUtils.hasText(query.searchText())) {
            String searchText = query.searchText().trim();
            predicate.and(part.name.containsIgnoreCase(searchText).or(part.description.containsIgnoreCase(searchText)));
        }

        List<Part> parts = queryFactory
                .selectFrom(part)
                .leftJoin(part.tags, tag)
                .fetchJoin()
                .where(predicate)
                .orderBy(part.name.asc())
                .distinct()
                .fetch();

        return new ListCategoryPartsQuery.CategoryParts(
                query.categoryId(),
                categoryName,
                parts.stream()
                        .map(partEntity -> new ListCategoryPartsQuery.PartRow(
                                partEntity.getId(),
                                partEntity.getName(),
                                toDescriptionSnippet(partEntity.getDescription()),
                                partEntity.getTags().stream()
                                        .sorted(Comparator.comparing(
                                                tagEntity -> tagEntity.getName().toLowerCase(Locale.ROOT)))
                                        .map(tagEntity -> new ListCategoryPartsQuery.TagCard(
                                                tagEntity.getName(),
                                                tagEntity.getColor().name()))
                                        .toList()))
                        .toList());
    }

    private String toDescriptionSnippet(String description) {
        if (!StringUtils.hasText(description)) {
            return "";
        }

        String normalized = description.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= DESCRIPTION_SNIPPET_LENGTH) {
            return normalized;
        }

        return normalized.substring(0, DESCRIPTION_SNIPPET_LENGTH - 1) + "…";
    }
}
