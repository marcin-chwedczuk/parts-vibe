package app.partsvibe.users.commands.invite;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserInviteTestDataBuilder.aUserInvite;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.RoleNames;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.domain.invite.UserInvite;
import app.partsvibe.users.errors.InvalidOrExpiredCredentialTokenException;
import app.partsvibe.users.errors.PasswordsDoNotMatchException;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.invite.UserInviteRepository;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

class FinalizeUserInviteCommandHandlerIT extends AbstractUsersIntegrationTest {
    private static final Instant NOW_2026_02_25T12_00Z = Instant.parse("2026-02-25T12:00:00Z");

    @Autowired
    private FinalizeUserInviteCommandHandler commandHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserInviteRepository userInviteRepository;

    @Autowired
    private CredentialTokenCodec tokenCodec;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    protected void beforeEachTest(TestInfo testInfo) {
        timeProvider.setNow(NOW_2026_02_25T12_00Z);
        roleRepository
                .findByName(RoleNames.USER)
                .orElseGet(() ->
                        roleRepository.save(aRole().withName(RoleNames.USER).build()));
    }

    @Test
    void createsUserWhenUsingInviteTokenAndMarksInviteUsed() {
        // given
        Instant now = NOW_2026_02_25T12_00Z;
        String tokenHash = tokenCodec.hash("invite-token");

        userInviteRepository.save(aUserInvite()
                .withEmail("invited-user@example.com")
                .withRoleName(RoleNames.USER)
                .withTokenHash(tokenHash)
                .withExpiresAt(now.plusSeconds(3600))
                .build());

        // when
        commandHandler.handle(
                new FinalizeUserInviteCommand("invite-token", "new-secure-password", "new-secure-password"));

        // then
        entityManager.flush();
        entityManager.clear();

        User savedUser = userRepository
                .findByUsernameIgnoreCase("invited-user@example.com")
                .orElseThrow();
        assertThat(passwordEncoder.matches("new-secure-password", savedUser.getPasswordHash()))
                .isTrue();
        assertThat(savedUser.getRoles()).extracting(Role::getName).containsExactly(RoleNames.USER);

        UserInvite savedInvite = userInviteRepository.findByTokenHash(tokenHash).orElseThrow();
        assertThat(savedInvite.getUsedAt()).isEqualTo(now);
        assertThat(savedInvite.getRevokedAt()).isNull();
    }

    @Test
    void rejectsWhenInviteTokenMissing() {
        // given
        // when / then
        assertThatThrownBy(() -> commandHandler.handle(
                        new FinalizeUserInviteCommand("missing-token", "new-secure-password", "new-secure-password")))
                .isInstanceOf(InvalidOrExpiredCredentialTokenException.class);
    }

    @Test
    void rejectsWhenInviteTokenExpired() {
        // given
        Instant now = NOW_2026_02_25T12_00Z;
        userInviteRepository.save(aUserInvite()
                .withEmail("expired-invite@example.com")
                .withRoleName(RoleNames.USER)
                .withTokenHash(tokenCodec.hash("expired-token"))
                .withExpiresAt(now.minusSeconds(1))
                .build());

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(
                        new FinalizeUserInviteCommand("expired-token", "new-secure-password", "new-secure-password")))
                .isInstanceOf(InvalidOrExpiredCredentialTokenException.class);
    }

    @Test
    void rejectsWhenPasswordsDoNotMatch() {
        // given
        Instant now = NOW_2026_02_25T12_00Z;
        userInviteRepository.save(aUserInvite()
                .withEmail("invited-user2@example.com")
                .withRoleName(RoleNames.USER)
                .withTokenHash(tokenCodec.hash("invite-token-2"))
                .withExpiresAt(now.plusSeconds(3600))
                .build());

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(
                        new FinalizeUserInviteCommand("invite-token-2", "new-secure-password", "different-password")))
                .isInstanceOf(PasswordsDoNotMatchException.class);
        assertThat(userRepository.findByUsernameIgnoreCase("invited-user2@example.com"))
                .isEmpty();
    }

    @Test
    void rejectsWhenUserAlreadyExistsForInviteEmail() {
        // given
        Instant now = NOW_2026_02_25T12_00Z;
        String tokenHash = tokenCodec.hash("invite-token-3");
        userRepository.save(aUser().withUsername("already@example.com").build());
        userInviteRepository.save(aUserInvite()
                .withEmail("already@example.com")
                .withRoleName(RoleNames.USER)
                .withTokenHash(tokenHash)
                .withExpiresAt(now.plusSeconds(3600))
                .build());

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(
                        new FinalizeUserInviteCommand("invite-token-3", "new-secure-password", "new-secure-password")))
                .isInstanceOf(InvalidOrExpiredCredentialTokenException.class);

        entityManager.flush();
        entityManager.clear();
        UserInvite savedInvite = userInviteRepository.findByTokenHash(tokenHash).orElseThrow();
        assertThat(savedInvite.getRevokedAt()).isEqualTo(now);
        assertThat(savedInvite.getUsedAt()).isNull();
    }
}
