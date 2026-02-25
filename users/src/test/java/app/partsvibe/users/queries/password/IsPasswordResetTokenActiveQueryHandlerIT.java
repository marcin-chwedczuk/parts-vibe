package app.partsvibe.users.queries.password;

import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.users.domain.invite.UserInvite;
import app.partsvibe.users.domain.security.UserPasswordResetToken;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.invite.UserInviteRepository;
import app.partsvibe.users.repo.security.UserPasswordResetTokenRepository;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class IsPasswordResetTokenActiveQueryHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private IsPasswordResetTokenActiveQueryHandler queryHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserInviteRepository userInviteRepository;

    @Autowired
    private CredentialTokenCodec tokenCodec;

    @Test
    void returnsTrueForActivePasswordResetToken() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);

        var user = userRepository.save(
                aUser().withUsername("token-user@example.com").build());
        tokenRepository.save(new UserPasswordResetToken(user, tokenCodec.hash("active-token"), now.plusSeconds(3600)));

        // when
        boolean active = queryHandler.handle(new IsPasswordResetTokenActiveQuery(" active-token "));

        // then
        assertThat(active).isTrue();
    }

    @Test
    void returnsFalseForExpiredRevokedOrUnknownToken() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);

        var user = userRepository.save(
                aUser().withUsername("token-user-2@example.com").build());

        var expired = tokenRepository.save(
                new UserPasswordResetToken(user, tokenCodec.hash("expired-token"), now.minusSeconds(1)));
        expired.setRevokedAt(now.minusSeconds(10));
        tokenRepository.save(expired);

        // when
        boolean expiredActive = queryHandler.handle(new IsPasswordResetTokenActiveQuery("expired-token"));
        boolean missingActive = queryHandler.handle(new IsPasswordResetTokenActiveQuery("missing-token"));

        // then
        assertThat(expiredActive).isFalse();
        assertThat(missingActive).isFalse();
    }

    @Test
    void returnsTrueForActiveInviteToken() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);
        userInviteRepository.save(new UserInvite(
                "invite@example.com",
                "ROLE_USER",
                null,
                tokenCodec.hash("invite-active-token"),
                now.plusSeconds(3600)));

        // when
        boolean active = queryHandler.handle(new IsPasswordResetTokenActiveQuery("invite-active-token"));

        // then
        assertThat(active).isTrue();
    }
}
