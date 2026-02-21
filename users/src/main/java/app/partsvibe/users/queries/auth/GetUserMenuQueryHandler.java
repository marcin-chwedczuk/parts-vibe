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
                .select(user.username, user.avatarId)
                .from(user)
                .where(user.username.eq(query.username()))
                .fetchOne();

        if (tuple == null) {
            throw new UserNotFoundException(query.username());
        }

        return new GetUserMenuQuery.UserMenuData(tuple.get(user.username), tuple.get(user.avatarId));
    }
}
