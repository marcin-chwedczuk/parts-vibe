package app.partsvibe.users.queries.usermanagement;

import static java.util.Comparator.naturalOrder;

import app.partsvibe.shared.cqrs.BaseQueryHandler;
import app.partsvibe.users.domain.QUser;
import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.errors.UserNotFoundException;
import app.partsvibe.users.models.UserDetailsModel;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Component;

@Component
class UserByIdQueryHandler extends BaseQueryHandler<UserByIdQuery, UserDetailsModel> {
    private final JPAQueryFactory queryFactory;

    UserByIdQueryHandler(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    protected UserDetailsModel doHandle(UserByIdQuery query) {
        QUser user = QUser.user;
        User userEntity =
                queryFactory.selectFrom(user).where(user.id.eq(query.userId())).fetchOne();
        if (userEntity == null) {
            throw new UserNotFoundException(query.userId());
        }

        return new UserDetailsModel(
                userEntity.getId(),
                userEntity.getUsername(),
                userEntity.isEnabled(),
                userEntity.getRoles().stream()
                        .map(Role::getName)
                        .sorted(naturalOrder())
                        .toList());
    }
}
