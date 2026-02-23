package app.partsvibe.users.queries.profile;

import app.partsvibe.shared.cqrs.BaseQueryHandler;
import app.partsvibe.users.domain.QUser;
import app.partsvibe.users.errors.UserNotFoundException;
import app.partsvibe.users.models.UserProfileModel;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Component;

@Component
class GetCurrentUserProfileQueryHandler extends BaseQueryHandler<GetCurrentUserProfileQuery, UserProfileModel> {
    private final JPAQueryFactory queryFactory;

    GetCurrentUserProfileQueryHandler(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    protected UserProfileModel doHandle(GetCurrentUserProfileQuery query) {
        QUser user = QUser.user;
        var tuple = queryFactory
                .select(user.id, user.username, user.bio, user.website, user.avatarId)
                .from(user)
                .where(user.id.eq(query.userId()))
                .fetchOne();

        if (tuple == null) {
            throw new UserNotFoundException(query.userId());
        }

        return new UserProfileModel(
                tuple.get(user.id),
                tuple.get(user.username),
                tuple.get(user.bio),
                tuple.get(user.website),
                tuple.get(user.avatarId));
    }
}
