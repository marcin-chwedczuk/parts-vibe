package app.partsvibe.users.queries.password;

import static app.partsvibe.users.test.databuilders.UserInviteTestDataBuilder.aUserInvite;
import static app.partsvibe.users.test.databuilders.UserPasswordResetTokenTestDataBuilder.aUserPasswordResetToken;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.users.domain.RoleNames;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.invite.UserInviteRepository;
import app.partsvibe.users.repo.security.UserPasswordResetTokenRepository;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

class ResolveInviteTokenContextQueryHandlerIT extends AbstractUsersIntegrationTest {
    private static final Instant NOW_2026_02_25T12_00Z = Instant.parse("2026-02-25T12:00:00Z");

    @Autowired
    private ResolveInviteTokenContextQueryHandler queryHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPasswordResetTokenRepository resetTokenRepository;

    @Autowired
    private UserInviteRepository userInviteRepository;

    @Autowired
    private CredentialTokenCodec tokenCodec;

    @Override
    protected void beforeEachTest(TestInfo testInfo) {
        timeProvider.setNow(NOW_2026_02_25T12_00Z);
    }

    @Test
    void returnsActiveContextForActiveInviteToken() {
        // given
        Instant now = NOW_2026_02_25T12_00Z;
        userInviteRepository.save(aUserInvite()
                .withEmail("invite-user@example.com")
                .withRoleName(RoleNames.USER)
                .withTokenHash(tokenCodec.hash("invite-token"))
                .withExpiresAt(now.plusSeconds(3600))
                .build());

        // when
        var result = queryHandler.handle(new ResolveInviteTokenContextQuery("invite-token"));

        // then
        assertThat(result).isPresent();
        assertThat(result.get().username()).isEqualTo("invite-user@example.com");
        assertThat(result.get().mode()).isEqualTo(ResolveInviteTokenContextQuery.InviteTokenMode.ACTIVE);
    }

    @Test
    void returnsAlreadyRegisteredContextForUsedInviteTokenWhenUserExists() {
        // given
        Instant now = NOW_2026_02_25T12_00Z;
        userRepository.save(aUser().withUsername("invite-user2@example.com").build());
        userInviteRepository.save(aUserInvite()
                .withEmail("invite-user2@example.com")
                .withRoleName(RoleNames.USER)
                .withTokenHash(tokenCodec.hash("invite-used-token"))
                .withExpiresAt(now.plusSeconds(3600))
                .withUsedAt(now.minusSeconds(10))
                .build());

        // when
        var result = queryHandler.handle(new ResolveInviteTokenContextQuery("invite-used-token"));

        // then
        assertThat(result).isPresent();
        assertThat(result.get().username()).isEqualTo("invite-user2@example.com");
        assertThat(result.get().mode()).isEqualTo(ResolveInviteTokenContextQuery.InviteTokenMode.ALREADY_REGISTERED);
    }

    @Test
    void returnsEmptyForPasswordResetToken() {
        // given
        Instant now = NOW_2026_02_25T12_00Z;
        var user = userRepository.save(
                aUser().withUsername("reset-user@example.com").build());
        resetTokenRepository.save(aUserPasswordResetToken()
                .withUser(user)
                .withTokenHash(tokenCodec.hash("reset-token"))
                .withExpiresAt(now.plusSeconds(3600))
                .build());

        // when
        var result = queryHandler.handle(new ResolveInviteTokenContextQuery("reset-token"));

        // then
        assertThat(result).isEmpty();
    }
}
