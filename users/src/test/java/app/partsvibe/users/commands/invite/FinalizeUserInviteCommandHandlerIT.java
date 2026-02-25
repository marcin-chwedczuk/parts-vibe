package app.partsvibe.users.commands.invite;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.users.domain.Role;
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
import org.springframework.beans.factory.annotation.Autowired;

class FinalizeUserInviteCommandHandlerIT extends AbstractUsersIntegrationTest {
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

    @Test
    void createsUserWhenUsingInviteTokenAndMarksInviteUsed() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);

        roleRepository.save(aRole().withName("ROLE_USER").build());
        userInviteRepository.save(new UserInvite(
                "invited-user@example.com", "ROLE_USER", null, tokenCodec.hash("invite-token"), now.plusSeconds(3600)));

        // when
        commandHandler.handle(
                new FinalizeUserInviteCommand("invite-token", "new-secure-password", "new-secure-password"));

        // then
        entityManager.flush();
        entityManager.clear();

        User savedUser = userRepository
                .findByUsernameIgnoreCase("invited-user@example.com")
                .orElseThrow();
        assertThat(savedUser.getPasswordHash()).isEqualTo("new-secure-password");
        assertThat(savedUser.getRoles()).extracting(Role::getName).containsExactly("ROLE_USER");

        UserInvite savedInvite = userInviteRepository.findAll().stream()
                .filter(invite -> invite.getEmail().equals("invited-user@example.com"))
                .findFirst()
                .orElseThrow();
        assertThat(savedInvite.getUsedAt()).isEqualTo(now);
        assertThat(savedInvite.getRevokedAt()).isNull();
    }

    @Test
    void rejectsWhenInviteTokenMissingOrExpired() {
        // given
        timeProvider.setNow(Instant.parse("2026-02-25T12:00:00Z"));

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(
                        new FinalizeUserInviteCommand("missing-token", "new-secure-password", "new-secure-password")))
                .isInstanceOf(InvalidOrExpiredCredentialTokenException.class);
    }

    @Test
    void rejectsWhenPasswordsDoNotMatch() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);
        roleRepository.save(aRole().withName("ROLE_USER").build());
        userInviteRepository.save(new UserInvite(
                "invited-user2@example.com",
                "ROLE_USER",
                null,
                tokenCodec.hash("invite-token-2"),
                now.plusSeconds(3600)));

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
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);
        roleRepository.save(aRole().withName("ROLE_USER").build());
        userRepository.save(aUser().withUsername("already@example.com").build());
        userInviteRepository.save(new UserInvite(
                "already@example.com", "ROLE_USER", null, tokenCodec.hash("invite-token-3"), now.plusSeconds(3600)));

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(
                        new FinalizeUserInviteCommand("invite-token-3", "new-secure-password", "new-secure-password")))
                .isInstanceOf(InvalidOrExpiredCredentialTokenException.class);
    }
}
