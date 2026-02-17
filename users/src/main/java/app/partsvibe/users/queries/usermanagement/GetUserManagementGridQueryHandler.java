package app.partsvibe.users.queries.usermanagement;

import app.partsvibe.shared.cqrs.BaseQueryHandler;
import app.partsvibe.users.domain.QUserAccount;
import app.partsvibe.users.web.form.UserManagementFilters;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
class GetUserManagementGridQueryHandler
        extends BaseQueryHandler<GetUserManagementGridQuery, GetUserManagementGridQueryResult> {
    private final JPAQueryFactory queryFactory;

    GetUserManagementGridQueryHandler(EntityManager entityManager) {
        this.queryFactory = new JPAQueryFactory(entityManager);
    }

    @Override
    protected GetUserManagementGridQueryResult doHandle(GetUserManagementGridQuery query) {
        QUserAccount user = QUserAccount.userAccount;
        BooleanBuilder predicate = buildPredicate(query, user);
        int safeSize = query.size() > 0 ? query.size() : 10;

        Long totalRowsValue =
                queryFactory.select(user.id.count()).from(user).where(predicate).fetchOne();
        long totalRows = totalRowsValue == null ? 0L : totalRowsValue;
        int totalPages = computeTotalPages(totalRows, safeSize);
        int safePage = Math.min(Math.max(1, query.page()), totalPages);

        List<GetUserManagementGridQueryResult.UserRow> rows = queryFactory
                .selectFrom(user)
                .where(predicate)
                .orderBy(orderBy(query, user))
                .offset((long) (safePage - 1) * safeSize)
                .limit(safeSize)
                .fetch()
                .stream()
                .map(userAccount -> new GetUserManagementGridQueryResult.UserRow(
                        userAccount.getId(),
                        userAccount.getUsername(),
                        userAccount.isEnabled(),
                        userAccount.getRoles().stream()
                                .map(role -> role.getName())
                                .sorted(Comparator.naturalOrder())
                                .toList()))
                .toList();

        int startRow = totalRows == 0 ? 0 : ((safePage - 1) * safeSize) + 1;
        int endRow = totalRows == 0 ? 0 : startRow + rows.size() - 1;

        return new GetUserManagementGridQueryResult(rows, totalRows, totalPages, safePage, startRow, endRow);
    }

    private BooleanBuilder buildPredicate(GetUserManagementGridQuery query, QUserAccount user) {
        BooleanBuilder predicate = new BooleanBuilder();

        if (query.username() != null && !query.username().isBlank()) {
            predicate.and(user.username.containsIgnoreCase(query.username().trim()));
        }
        if (UserManagementFilters.ENABLED_ENABLED.equals(query.enabled())) {
            predicate.and(user.enabled.isTrue());
        } else if (UserManagementFilters.ENABLED_DISABLED.equals(query.enabled())) {
            predicate.and(user.enabled.isFalse());
        }

        List<String> roles = query.roles() == null ? List.of() : query.roles();
        for (String role : roles) {
            predicate.and(user.roles.any().name.eq(role));
        }

        return predicate;
    }

    private int computeTotalPages(long totalRows, int size) {
        return Math.max(1, (int) Math.ceil(totalRows / (double) size));
    }

    private OrderSpecifier<?> orderBy(GetUserManagementGridQuery query, QUserAccount user) {
        String sortBy = query.sortBy() == null ? UserManagementFilters.SORT_NONE : query.sortBy();
        String sortDir = query.sortDir() == null
                ? UserManagementFilters.SORT_ASC
                : query.sortDir().toLowerCase(Locale.ROOT);
        Order direction = UserManagementFilters.SORT_DESC.equals(sortDir) ? Order.DESC : Order.ASC;

        if (UserManagementFilters.SORT_BY_USERNAME.equals(sortBy)) {
            return new OrderSpecifier<>(direction, user.username);
        }
        if (UserManagementFilters.SORT_BY_ENABLED.equals(sortBy)) {
            return new OrderSpecifier<>(direction, user.enabled);
        }

        return new OrderSpecifier<>(Order.ASC, user.id);
    }
}
