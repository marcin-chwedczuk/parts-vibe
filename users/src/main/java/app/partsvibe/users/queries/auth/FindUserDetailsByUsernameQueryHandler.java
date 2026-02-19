package app.partsvibe.users.queries.auth;

import app.partsvibe.shared.cqrs.BaseQueryHandler;
import app.partsvibe.users.domain.QUser;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.security.UserPrincipal;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
class FindUserDetailsByUsernameQueryHandler extends BaseQueryHandler<FindUserDetailsByUsernameQuery, UserDetails> {
    private static final Logger log = LoggerFactory.getLogger(FindUserDetailsByUsernameQueryHandler.class);

    private final JPAQueryFactory queryFactory;

    FindUserDetailsByUsernameQueryHandler(JPAQueryFactory queryFactory) {
        this.queryFactory = queryFactory;
    }

    @Override
    protected UserDetails doHandle(FindUserDetailsByUsernameQuery query) {
        QUser user = QUser.user;
        log.info("Loading user details for username={}", query.username());
        User userDetails = queryFactory
                .selectFrom(user)
                .where(user.username.eq(query.username()))
                .fetchOne();

        if (userDetails == null) {
            log.warn("User not found: {}", query.username());
            throw new UsernameNotFoundException("User not found: " + query.username());
        }

        return new UserPrincipal(userDetails);
    }
}
