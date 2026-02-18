package app.partsvibe.users.queries.usermanagement;

import static app.partsvibe.users.queries.usermanagement.GetUserManagementGridQuery.*;
import static java.util.Comparator.naturalOrder;

import app.partsvibe.shared.cqrs.BasePaginatedQueryHandler;
import app.partsvibe.shared.cqrs.PageResult;
import app.partsvibe.users.domain.QUserAccount;
import app.partsvibe.users.domain.Role;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
class GetUserManagementGridQueryHandler
        extends BasePaginatedQueryHandler<GetUserManagementGridQuery, PageResult<GetUserManagementGridQuery.User>> {
    private final JPAQueryFactory queryFactory;

    GetUserManagementGridQueryHandler(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    protected PageResult<GetUserManagementGridQuery.User> doHandle(GetUserManagementGridQuery query) {
        QUserAccount user = QUserAccount.userAccount;
        BooleanBuilder predicate = buildPredicate(query, user);
        int safeSize = resolvePageSize(query);

        Long totalRowsValue =
                queryFactory.select(user.id.count()).from(user).where(predicate).fetchOne();
        long totalRows = totalRowsValue == null ? 0L : totalRowsValue;
        int totalPages = computeTotalPages(totalRows, safeSize);
        int safePage = resolvePageNumber(query, totalPages);

        List<GetUserManagementGridQuery.User> rows = queryFactory
                .selectFrom(user)
                .where(predicate)
                .orderBy(orderBy(query, user))
                .offset((long) (safePage - 1) * safeSize)
                .limit(safeSize)
                .fetch()
                .stream()
                // TODO: Avoid potential N+1 on roles by switching to a paged-id + batched roles projection query.
                .map(userAccount -> new GetUserManagementGridQuery.User(
                        userAccount.getId(),
                        userAccount.getUsername(),
                        userAccount.isEnabled(),
                        userAccount.getRoles().stream()
                                .map(Role::getName)
                                .sorted(naturalOrder())
                                .toList()))
                .toList();

        return new PageResult<>(rows, totalRows, totalPages, safePage, safeSize);
    }

    private BooleanBuilder buildPredicate(GetUserManagementGridQuery query, QUserAccount user) {
        BooleanBuilder predicate = new BooleanBuilder();

        if (query.username() != null && !query.username().isBlank()) {
            predicate.and(user.username.containsIgnoreCase(query.username().trim()));
        }
        if (ENABLED_ENABLED.equals(query.enabled())) {
            predicate.and(user.enabled.isTrue());
        } else if (ENABLED_DISABLED.equals(query.enabled())) {
            predicate.and(user.enabled.isFalse());
        }

        List<String> roles = query.roles() == null ? List.of() : query.roles();
        for (String role : roles) {
            predicate.and(user.roles.any().name.eq(role));
        }

        return predicate;
    }

    private OrderSpecifier<?>[] orderBy(GetUserManagementGridQuery query, QUserAccount user) {
        String sortBy = query.sortBy() == null ? SORT_NONE : query.sortBy();
        String sortDir = query.sortDir() == null ? SORT_ASC : query.sortDir().toLowerCase(Locale.ROOT);
        Order direction = SORT_DESC.equals(sortDir) ? Order.DESC : Order.ASC;

        if (SORT_BY_USERNAME.equals(sortBy)) {
            return new OrderSpecifier[] {
                new OrderSpecifier<>(direction, user.username), new OrderSpecifier<>(Order.ASC, user.id)
            };
        }
        if (SORT_BY_ENABLED.equals(sortBy)) {
            return new OrderSpecifier[] {
                new OrderSpecifier<>(direction, user.enabled), new OrderSpecifier<>(Order.ASC, user.id)
            };
        }

        return new OrderSpecifier[] {new OrderSpecifier<>(Order.ASC, user.id)};
    }
}
