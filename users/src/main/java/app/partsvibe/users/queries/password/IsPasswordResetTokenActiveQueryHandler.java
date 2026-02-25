package app.partsvibe.users.queries.password;

import app.partsvibe.shared.cqrs.BaseQueryHandler;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.users.domain.invite.QUserInvite;
import app.partsvibe.users.domain.security.QUserPasswordResetToken;
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
        var credentialToken = QUserPasswordResetToken.userPasswordResetToken;
        var invite = QUserInvite.userInvite;
        var now = timeProvider.now();

        Long passwordResetTokenCount = queryFactory
                .select(credentialToken.id.count())
                .from(credentialToken)
                .where(credentialToken
                        .tokenHash
                        .eq(tokenHash)
                        .and(credentialToken.usedAt.isNull())
                        .and(credentialToken.revokedAt.isNull())
                        .and(credentialToken.expiresAt.after(now)))
                .fetchOne();

        if (passwordResetTokenCount != null && passwordResetTokenCount > 0) {
            return true;
        }

        Long activeInviteCount = queryFactory
                .select(invite.id.count())
                .from(invite)
                .where(invite.tokenHash
                        .eq(tokenHash)
                        .and(invite.usedAt.isNull())
                        .and(invite.revokedAt.isNull())
                        .and(invite.expiresAt.after(now)))
                .fetchOne();

        return activeInviteCount != null && activeInviteCount > 0;
    }
}
