package app.partsvibe.users.commands.password;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.domain.invite.UserInvite;
import app.partsvibe.users.domain.security.UserPasswordResetToken;
import app.partsvibe.users.errors.InvalidOrExpiredCredentialTokenException;
import app.partsvibe.users.errors.WeakPasswordException;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.invite.UserInviteRepository;
import app.partsvibe.users.repo.security.UserPasswordResetTokenRepository;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ResetPasswordWithTokenCommandHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private ResetPasswordWithTokenCommandHandler commandHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserPasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserInviteRepository userInviteRepository;

    @Autowired
    private CredentialTokenCodec tokenCodec;

    @Autowired
    private EntityManager entityManager;

    @Test
    void changesPasswordMarksTokenUsedAndRevokesOtherPasswordResetTokens() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);

        Role roleUser = roleRepository.save(aRole().withName("ROLE_USER").build());
        User user = userRepository.save(aUser().withUsername("alice@example.com")
                .withPasswordHash("old-password")
                .withRole(roleUser)
                .build());

        String rawToken = "reset-token-primary";
        UserPasswordResetToken usedToken = tokenRepository.save(
                new UserPasswordResetToken(user, tokenCodec.hash(rawToken), now.plusSeconds(3600)));

        UserPasswordResetToken otherResetToken = tokenRepository.save(
                new UserPasswordResetToken(user, tokenCodec.hash("reset-token-secondary"), now.plusSeconds(3600)));

        // when
        commandHandler.handle(
                new ResetPasswordWithTokenCommand(rawToken, "new-secure-password", "new-secure-password"));

        // then
        entityManager.flush();
        entityManager.clear();

        User savedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(savedUser.getPasswordHash()).isEqualTo("new-secure-password");

        UserPasswordResetToken savedUsedToken =
                tokenRepository.findById(usedToken.getId()).orElseThrow();
        assertThat(savedUsedToken.getUsedAt()).isEqualTo(now);
        assertThat(savedUsedToken.getRevokedAt()).isNull();

        UserPasswordResetToken savedOtherReset =
                tokenRepository.findById(otherResetToken.getId()).orElseThrow();
        assertThat(savedOtherReset.getRevokedAt()).isEqualTo(now);
    }

    @Test
    void throwsWhenTokenIsMissingOrExpired() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);

        Role roleUser = roleRepository.save(aRole().withName("ROLE_USER").build());
        User user = userRepository.save(aUser().withUsername("alice@example.com")
                .withPasswordHash("old-password")
                .withRole(roleUser)
                .build());

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(new ResetPasswordWithTokenCommand(
                        "missing-token", "new-secure-password", "new-secure-password")))
                .isInstanceOf(InvalidOrExpiredCredentialTokenException.class);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getPasswordHash())
                .isEqualTo("old-password");
    }

    @Test
    void throwsWhenPasswordsDoNotMatch() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);

        Role roleUser = roleRepository.save(aRole().withName("ROLE_USER").build());
        User user = userRepository.save(aUser().withUsername("alice@example.com")
                .withPasswordHash("old-password")
                .withRole(roleUser)
                .build());

        String rawToken = "reset-token-primary";
        tokenRepository.save(new UserPasswordResetToken(user, tokenCodec.hash(rawToken), now.plusSeconds(3600)));

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(
                        new ResetPasswordWithTokenCommand(rawToken, "new-secure-password", "different-password")))
                .isInstanceOf(WeakPasswordException.class);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getPasswordHash())
                .isEqualTo("old-password");
    }

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
                new ResetPasswordWithTokenCommand("invite-token", "new-secure-password", "new-secure-password"));

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
}
