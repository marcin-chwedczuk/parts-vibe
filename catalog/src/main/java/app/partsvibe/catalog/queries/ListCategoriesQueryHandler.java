package app.partsvibe.catalog.queries;

import app.partsvibe.catalog.domain.Category;
import app.partsvibe.catalog.domain.QCategory;
import app.partsvibe.catalog.domain.QPart;
import app.partsvibe.catalog.domain.QTag;
import app.partsvibe.shared.cqrs.BaseQueryHandler;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
class ListCategoriesQueryHandler extends BaseQueryHandler<ListCategoriesQuery, List<ListCategoriesQuery.CategoryCard>> {
    private final JPAQueryFactory queryFactory;

    ListCategoriesQueryHandler(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    protected List<ListCategoriesQuery.CategoryCard> doHandle(ListCategoriesQuery query) {
        QCategory category = QCategory.category;
        QTag tag = QTag.tag;
        QPart part = QPart.part;

        List<Category> categories = queryFactory
                .selectFrom(category)
                .leftJoin(category.tags, tag)
                .fetchJoin()
                .orderBy(category.name.asc())
                .distinct()
                .fetch();

        Map<Long, Long> partsCountByCategoryId =
                queryFactory
                        .select(part.category.id, part.id.count())
                        .from(part)
                        .groupBy(part.category.id)
                        .fetch()
                        .stream()
                        .collect(java.util.stream.Collectors.toMap(
                                tuple -> tuple.get(part.category.id),
                                tuple -> tuple.get(part.id.count()),
                                (left, right) -> left));

        return categories.stream()
                .map(it -> new ListCategoriesQuery.CategoryCard(
                        it.getId(),
                        it.getName(),
                        partsCountByCategoryId.getOrDefault(it.getId(), 0L),
                        it.getTags().stream()
                                .sorted(Comparator.comparing(
                                        tagEntity -> tagEntity.getName().toLowerCase(Locale.ROOT)))
                                .map(tagEntity -> new ListCategoriesQuery.TagCard(
                                        tagEntity.getName(),
                                        tagEntity.getColor().name()))
                                .toList()))
                .toList();
    }
}
