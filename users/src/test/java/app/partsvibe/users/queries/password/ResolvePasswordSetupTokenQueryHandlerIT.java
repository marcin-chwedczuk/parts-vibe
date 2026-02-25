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

class ResolvePasswordSetupTokenQueryHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private ResolvePasswordSetupTokenQueryHandler queryHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPasswordResetTokenRepository resetTokenRepository;

    @Autowired
    private UserInviteRepository userInviteRepository;

    @Autowired
    private CredentialTokenCodec tokenCodec;

    @Test
    void returnsPasswordResetContextForActivePasswordResetToken() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);

        var user = userRepository.save(
                aUser().withUsername("reset-user@example.com").build());
        resetTokenRepository.save(
                new UserPasswordResetToken(user, tokenCodec.hash("reset-token"), now.plusSeconds(3600)));

        // when
        var result = queryHandler.handle(new ResolvePasswordSetupTokenQuery("reset-token"));

        // then
        assertThat(result).isPresent();
        assertThat(result.get().username()).isEqualTo("reset-user@example.com");
        assertThat(result.get().mode()).isEqualTo(ResolvePasswordSetupTokenQuery.SetupMode.PASSWORD_RESET);
    }

    @Test
    void returnsInviteContextForActiveInviteToken() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);
        userInviteRepository.save(new UserInvite(
                "invite-user@example.com", "ROLE_USER", null, tokenCodec.hash("invite-token"), now.plusSeconds(3600)));

        // when
        var result = queryHandler.handle(new ResolvePasswordSetupTokenQuery("invite-token"));

        // then
        assertThat(result).isPresent();
        assertThat(result.get().username()).isEqualTo("invite-user@example.com");
        assertThat(result.get().mode()).isEqualTo(ResolvePasswordSetupTokenQuery.SetupMode.INVITE);
    }

    @Test
    void returnsInviteAlreadyRegisteredContextForUsedInviteTokenWhenUserExists() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);
        userRepository.save(aUser().withUsername("invite-user2@example.com").build());
        var invite = userInviteRepository.save(new UserInvite(
                "invite-user2@example.com",
                "ROLE_USER",
                null,
                tokenCodec.hash("invite-used-token"),
                now.plusSeconds(3600)));
        invite.setUsedAt(now.minusSeconds(10));
        userInviteRepository.save(invite);

        // when
        var result = queryHandler.handle(new ResolvePasswordSetupTokenQuery("invite-used-token"));

        // then
        assertThat(result).isPresent();
        assertThat(result.get().username()).isEqualTo("invite-user2@example.com");
        assertThat(result.get().mode()).isEqualTo(ResolvePasswordSetupTokenQuery.SetupMode.INVITE_ALREADY_REGISTERED);
    }
}
