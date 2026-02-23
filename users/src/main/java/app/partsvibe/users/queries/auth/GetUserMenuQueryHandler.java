package app.partsvibe.users.queries.auth;

import app.partsvibe.shared.cqrs.BaseQueryHandler;
import app.partsvibe.users.domain.QUser;
import app.partsvibe.users.errors.UserNotFoundException;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Component;

@Component
class GetUserMenuQueryHandler extends BaseQueryHandler<GetUserMenuQuery, GetUserMenuQuery.UserMenuData> {
    private final JPAQueryFactory queryFactory;

    GetUserMenuQueryHandler(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    protected GetUserMenuQuery.UserMenuData doHandle(GetUserMenuQuery query) {
        QUser user = QUser.user;
        var tuple = queryFactory
                .select(user.id, user.avatarId)
                .from(user)
                .where(user.id.eq(query.userId()))
                .fetchOne();
        if (tuple == null) {
            throw new UserNotFoundException(query.userId());
        }

        return new GetUserMenuQuery.UserMenuData(tuple.get(user.avatarId));
    }
}
