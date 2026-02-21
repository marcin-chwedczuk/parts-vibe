package app.partsvibe.catalog.queries;

import app.partsvibe.catalog.domain.Part;
import app.partsvibe.catalog.domain.QPart;
import app.partsvibe.catalog.domain.QTag;
import app.partsvibe.catalog.errors.PartNotFoundException;
import app.partsvibe.shared.cqrs.BaseQueryHandler;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Comparator;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
class PartByIdQueryHandler extends BaseQueryHandler<PartByIdQuery, PartByIdQuery.PartDetails> {
    private final JPAQueryFactory queryFactory;

    PartByIdQueryHandler(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    protected PartByIdQuery.PartDetails doHandle(PartByIdQuery query) {
        QPart part = QPart.part;
        QTag tag = QTag.tag;

        Part partEntity = queryFactory
                .selectFrom(part)
                .leftJoin(part.category)
                .fetchJoin()
                .leftJoin(part.tags, tag)
                .fetchJoin()
                .where(part.id.eq(query.partId()))
                .distinct()
                .fetchOne();

        if (partEntity == null) {
            throw new PartNotFoundException(query.partId());
        }

        return new PartByIdQuery.PartDetails(
                partEntity.getId(),
                partEntity.getName(),
                partEntity.getDescription(),
                partEntity.getCategory().getId(),
                partEntity.getCategory().getName(),
                partEntity.getTags().stream()
                        .sorted(Comparator.comparing(it -> it.getName().toLowerCase(Locale.ROOT)))
                        .map(it -> new PartByIdQuery.TagCard(
                                it.getName(), it.getColor().name()))
                        .toList());
    }
}
