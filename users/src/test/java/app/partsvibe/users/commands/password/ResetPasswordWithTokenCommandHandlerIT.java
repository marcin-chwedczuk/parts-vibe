package app.partsvibe.users.commands.password;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.domain.security.UserCredentialToken;
import app.partsvibe.users.domain.security.UserCredentialTokenPurpose;
import app.partsvibe.users.errors.InvalidOrExpiredCredentialTokenException;
import app.partsvibe.users.errors.WeakPasswordException;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.security.UserCredentialTokenRepository;
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
    private UserCredentialTokenRepository tokenRepository;

    @Autowired
    private CredentialTokenCodec tokenCodec;

    @Autowired
    private EntityManager entityManager;

    @Test
    void changesPasswordMarksTokenUsedAndRevokesOtherActiveTokens() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);

        Role roleUser = roleRepository.save(aRole().withName("ROLE_USER").build());
        User user = userRepository.save(aUser().withUsername("alice@example.com")
                .withPasswordHash("old-password")
                .withRole(roleUser)
                .build());

        String rawToken = "reset-token-primary";
        UserCredentialToken usedToken = tokenRepository.save(new UserCredentialToken(
                user, tokenCodec.hash(rawToken), UserCredentialTokenPurpose.PASSWORD_RESET, now.plusSeconds(3600)));

        UserCredentialToken otherResetToken = tokenRepository.save(new UserCredentialToken(
                user,
                tokenCodec.hash("reset-token-secondary"),
                UserCredentialTokenPurpose.PASSWORD_RESET,
                now.plusSeconds(3600)));
        UserCredentialToken inviteToken = tokenRepository.save(new UserCredentialToken(
                user,
                tokenCodec.hash("invite-token"),
                UserCredentialTokenPurpose.INVITE_ACTIVATION,
                now.plusSeconds(3600)));

        // when
        commandHandler.handle(
                new ResetPasswordWithTokenCommand(rawToken, "new-secure-password", "new-secure-password"));

        // then
        entityManager.clear();

        User savedUser = userRepository.findById(user.getId()).orElseThrow();
        assertThat(savedUser.getPasswordHash()).isEqualTo("new-secure-password");

        UserCredentialToken savedUsedToken =
                tokenRepository.findById(usedToken.getId()).orElseThrow();
        assertThat(savedUsedToken.getUsedAt()).isEqualTo(now);
        assertThat(savedUsedToken.getRevokedAt()).isNull();

        UserCredentialToken savedOtherReset =
                tokenRepository.findById(otherResetToken.getId()).orElseThrow();
        assertThat(savedOtherReset.getRevokedAt()).isEqualTo(now);

        UserCredentialToken savedInvite =
                tokenRepository.findById(inviteToken.getId()).orElseThrow();
        assertThat(savedInvite.getRevokedAt()).isEqualTo(now);
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
        tokenRepository.save(new UserCredentialToken(
                user, tokenCodec.hash(rawToken), UserCredentialTokenPurpose.PASSWORD_RESET, now.plusSeconds(3600)));

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(
                        new ResetPasswordWithTokenCommand(rawToken, "new-secure-password", "different-password")))
                .isInstanceOf(WeakPasswordException.class);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getPasswordHash())
                .isEqualTo("old-password");
    }
}
