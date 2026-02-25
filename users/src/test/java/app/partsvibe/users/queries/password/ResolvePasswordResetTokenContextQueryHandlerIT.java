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

class ResolvePasswordResetTokenContextQueryHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private ResolvePasswordResetTokenContextQueryHandler queryHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPasswordResetTokenRepository resetTokenRepository;

    @Autowired
    private UserInviteRepository userInviteRepository;

    @Autowired
    private CredentialTokenCodec tokenCodec;

    @Test
    void returnsContextForActivePasswordResetToken() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);
        var user = userRepository.save(
                aUser().withUsername("reset-user@example.com").build());
        resetTokenRepository.save(
                new UserPasswordResetToken(user, tokenCodec.hash("reset-token"), now.plusSeconds(3600)));

        // when
        var result = queryHandler.handle(new ResolvePasswordResetTokenContextQuery("reset-token"));

        // then
        assertThat(result).isPresent();
        assertThat(result.get().username()).isEqualTo("reset-user@example.com");
    }

    @Test
    void returnsEmptyForInviteToken() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);
        userInviteRepository.save(new UserInvite(
                "invite-user@example.com", "ROLE_USER", null, tokenCodec.hash("invite-token"), now.plusSeconds(3600)));

        // when
        var result = queryHandler.handle(new ResolvePasswordResetTokenContextQuery("invite-token"));

        // then
        assertThat(result).isEmpty();
    }
}
