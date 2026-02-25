package app.partsvibe.users.queries.password;

import app.partsvibe.shared.cqrs.BaseQueryHandler;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.users.domain.QUser;
import app.partsvibe.users.domain.security.QUserPasswordResetToken;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class ResolvePasswordResetTokenContextQueryHandler
        extends BaseQueryHandler<
                ResolvePasswordResetTokenContextQuery, Optional<ResolvePasswordResetTokenContextQuery.TokenContext>> {
    private final JPAQueryFactory queryFactory;
    private final CredentialTokenCodec tokenCodec;
    private final TimeProvider timeProvider;

    ResolvePasswordResetTokenContextQueryHandler(
            JPAQueryFactory queryFactory, CredentialTokenCodec tokenCodec, TimeProvider timeProvider) {
        this.queryFactory = queryFactory;
        this.tokenCodec = tokenCodec;
        this.timeProvider = timeProvider;
    }

    @Override
    protected Optional<ResolvePasswordResetTokenContextQuery.TokenContext> doHandle(
            ResolvePasswordResetTokenContextQuery query) {
        String tokenHash = tokenCodec.hash(query.token().trim());
        var now = timeProvider.now();

        var resetToken = QUserPasswordResetToken.userPasswordResetToken;
        var user = QUser.user;
        String resetUsername = queryFactory
                .select(user.username)
                .from(resetToken)
                .join(resetToken.user, user)
                .where(resetToken
                        .tokenHash
                        .eq(tokenHash)
                        .and(resetToken.usedAt.isNull())
                        .and(resetToken.revokedAt.isNull())
                        .and(resetToken.expiresAt.after(now)))
                .fetchOne();

        if (resetUsername == null) {
            return Optional.empty();
        }
        return Optional.of(new ResolvePasswordResetTokenContextQuery.TokenContext(resetUsername));
    }
}
