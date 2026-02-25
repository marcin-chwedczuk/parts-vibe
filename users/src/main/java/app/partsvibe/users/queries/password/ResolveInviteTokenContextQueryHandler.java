package app.partsvibe.users.queries.password;

import app.partsvibe.shared.cqrs.BaseQueryHandler;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.users.domain.QUser;
import app.partsvibe.users.domain.invite.QUserInvite;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class ResolveInviteTokenContextQueryHandler
        extends BaseQueryHandler<
                ResolveInviteTokenContextQuery, Optional<ResolveInviteTokenContextQuery.TokenContext>> {
    private final JPAQueryFactory queryFactory;
    private final CredentialTokenCodec tokenCodec;
    private final TimeProvider timeProvider;

    ResolveInviteTokenContextQueryHandler(
            JPAQueryFactory queryFactory, CredentialTokenCodec tokenCodec, TimeProvider timeProvider) {
        this.queryFactory = queryFactory;
        this.tokenCodec = tokenCodec;
        this.timeProvider = timeProvider;
    }

    @Override
    protected Optional<ResolveInviteTokenContextQuery.TokenContext> doHandle(ResolveInviteTokenContextQuery query) {
        String tokenHash = tokenCodec.hash(query.token().trim());
        var now = timeProvider.now();

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
            return Optional.of(new ResolveInviteTokenContextQuery.TokenContext(
                    inviteEmail, ResolveInviteTokenContextQuery.InviteTokenMode.ACTIVE));
        }

        String anyInviteEmail = queryFactory
                .select(invite.email)
                .from(invite)
                .where(invite.tokenHash.eq(tokenHash))
                .fetchOne();
        if (anyInviteEmail != null) {
            var user = QUser.user;
            Long existingUserCount = queryFactory
                    .select(user.id.count())
                    .from(user)
                    .where(user.username.equalsIgnoreCase(anyInviteEmail))
                    .fetchOne();
            if (existingUserCount != null && existingUserCount > 0) {
                return Optional.of(new ResolveInviteTokenContextQuery.TokenContext(
                        anyInviteEmail, ResolveInviteTokenContextQuery.InviteTokenMode.ALREADY_REGISTERED));
            }
        }

        return Optional.empty();
    }
}
