package app.partsvibe.users.queries.password;

import app.partsvibe.shared.cqrs.BaseQueryHandler;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.users.domain.QUser;
import app.partsvibe.users.domain.security.QUserCredentialToken;
import app.partsvibe.users.domain.security.UserCredentialTokenPurpose;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Component;

@Component
class IsPasswordResetTokenActiveQueryHandler extends BaseQueryHandler<IsPasswordResetTokenActiveQuery, Boolean> {
    private final JPAQueryFactory queryFactory;
    private final CredentialTokenCodec tokenCodec;
    private final TimeProvider timeProvider;

    IsPasswordResetTokenActiveQueryHandler(
            JPAQueryFactory queryFactory, CredentialTokenCodec tokenCodec, TimeProvider timeProvider) {
        this.queryFactory = queryFactory;
        this.tokenCodec = tokenCodec;
        this.timeProvider = timeProvider;
    }

    @Override
    protected Boolean doHandle(IsPasswordResetTokenActiveQuery query) {
        String tokenHash = tokenCodec.hash(query.token().trim());
        var token = QUserCredentialToken.userCredentialToken;
        var user = QUser.user;

        Long count = queryFactory
                .select(token.id.count())
                .from(token)
                .join(token.user, user)
                .where(token.tokenHash
                        .eq(tokenHash)
                        .and(token.usedAt.isNull())
                        .and(token.revokedAt.isNull())
                        .and(token.expiresAt.after(timeProvider.now()))
                        .and(token.purpose.in(
                                UserCredentialTokenPurpose.PASSWORD_RESET,
                                UserCredentialTokenPurpose.INVITE_ACTIVATION)))
                .fetchOne();

        return count != null && count > 0;
    }
}
