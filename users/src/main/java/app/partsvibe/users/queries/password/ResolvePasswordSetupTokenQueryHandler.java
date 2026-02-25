package app.partsvibe.users.queries.password;

import app.partsvibe.shared.cqrs.BaseQueryHandler;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.users.domain.QUser;
import app.partsvibe.users.domain.invite.QUserInvite;
import app.partsvibe.users.domain.security.QUserPasswordResetToken;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class ResolvePasswordSetupTokenQueryHandler
        extends BaseQueryHandler<
                ResolvePasswordSetupTokenQuery, Optional<ResolvePasswordSetupTokenQuery.TokenContext>> {
    private final JPAQueryFactory queryFactory;
    private final CredentialTokenCodec tokenCodec;
    private final TimeProvider timeProvider;

    ResolvePasswordSetupTokenQueryHandler(
            JPAQueryFactory queryFactory, CredentialTokenCodec tokenCodec, TimeProvider timeProvider) {
        this.queryFactory = queryFactory;
        this.tokenCodec = tokenCodec;
        this.timeProvider = timeProvider;
    }

    @Override
    protected Optional<ResolvePasswordSetupTokenQuery.TokenContext> doHandle(ResolvePasswordSetupTokenQuery query) {
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
        if (resetUsername != null) {
            return Optional.of(new ResolvePasswordSetupTokenQuery.TokenContext(
                    resetUsername, ResolvePasswordSetupTokenQuery.SetupMode.PASSWORD_RESET));
        }

        var invite = QUserInvite.userInvite;
        String inviteEmail = queryFactory
                .select(invite.email)
                .from(invite)
                .where(invite.tokenHash
                        .eq(tokenHash)
                        .and(invite.usedAt.isNull())
                        .and(invite.revokedAt.isNull())
                        .and(invite.expiresAt.after(now)))
                .fetchOne();
        if (inviteEmail != null) {
            return Optional.of(new ResolvePasswordSetupTokenQuery.TokenContext(
                    inviteEmail, ResolvePasswordSetupTokenQuery.SetupMode.INVITE));
        }

        String anyInviteEmail = queryFactory
                .select(invite.email)
                .from(invite)
                .where(invite.tokenHash.eq(tokenHash))
                .fetchOne();
        if (anyInviteEmail != null) {
            Long existingUserCount = queryFactory
                    .select(user.id.count())
                    .from(user)
                    .where(user.username.equalsIgnoreCase(anyInviteEmail))
                    .fetchOne();
            if (existingUserCount != null && existingUserCount > 0) {
                return Optional.of(new ResolvePasswordSetupTokenQuery.TokenContext(
                        anyInviteEmail, ResolvePasswordSetupTokenQuery.SetupMode.INVITE_ALREADY_REGISTERED));
            }
        }

        return Optional.empty();
    }
}
