package app.partsvibe.users.commands.password;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserPasswordResetTokenTestDataBuilder.aUserPasswordResetToken;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.RoleNames;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.domain.security.UserPasswordResetToken;
import app.partsvibe.users.errors.InvalidOrExpiredCredentialTokenException;
import app.partsvibe.users.errors.PasswordsDoNotMatchException;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.security.UserPasswordResetTokenRepository;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

class ResetPasswordWithTokenCommandHandlerIT extends AbstractUsersIntegrationTest {
    private static final Instant NOW_2026_02_25T12_00Z = Instant.parse("2026-02-25T12:00:00Z");

    @Autowired
    private ResetPasswordWithTokenCommandHandler commandHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserPasswordResetTokenRepository tokenRepository;

    @Autowired
    private CredentialTokenCodec tokenCodec;

    @Autowired
    private EntityManager entityManager;

    @Override
    protected void beforeEachTest(TestInfo testInfo) {
        timeProvider.setNow(NOW_2026_02_25T12_00Z);
        roleRepository
                .findByName(RoleNames.USER)
                .orElseGet(() ->
                        roleRepository.save(aRole().withName(RoleNames.USER).build()));
    }

    @Test
    void changesPasswordMarksTokenUsedAndRevokesOtherPasswordResetTokens() {
        // given
        Instant now = NOW_2026_02_25T12_00Z;

        Role roleUser = roleRepository.findByName(RoleNames.USER).orElseThrow();
        User user = userRepository.save(aUser().withUsername("alice@example.com")
                .withPasswordHash("old-password")
                .withRole(roleUser)
                .build());

        String rawToken = "reset-token-primary";
        UserPasswordResetToken usedToken = tokenRepository.save(aUserPasswordResetToken()
                .withUser(user)
                .withTokenHash(tokenCodec.hash(rawToken))
                .withExpiresAt(now.plusSeconds(3600))
                .build());

        UserPasswordResetToken otherResetToken = tokenRepository.save(aUserPasswordResetToken()
                .withUser(user)
                .withTokenHash(tokenCodec.hash("reset-token-secondary"))
                .withExpiresAt(now.plusSeconds(3600))
                .build());

        // when
        commandHandler.handle(ResetPasswordWithTokenCommand.builder()
                .token(rawToken)
                .password("new-secure-password")
                .repeatedPassword("new-secure-password")
                .build());

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
        Role roleUser = roleRepository.findByName(RoleNames.USER).orElseThrow();
        User user = userRepository.save(aUser().withUsername("alice@example.com")
                .withPasswordHash("old-password")
                .withRole(roleUser)
                .build());

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(ResetPasswordWithTokenCommand.builder()
                        .token("missing-token")
                        .password("new-secure-password")
                        .repeatedPassword("new-secure-password")
                        .build()))
                .isInstanceOf(InvalidOrExpiredCredentialTokenException.class);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getPasswordHash())
                .isEqualTo("old-password");
    }

    @Test
    void throwsWhenPasswordsDoNotMatch() {
        // given
        Instant now = NOW_2026_02_25T12_00Z;

        Role roleUser = roleRepository.findByName(RoleNames.USER).orElseThrow();
        User user = userRepository.save(aUser().withUsername("alice@example.com")
                .withPasswordHash("old-password")
                .withRole(roleUser)
                .build());

        String rawToken = "reset-token-primary";
        tokenRepository.save(aUserPasswordResetToken()
                .withUser(user)
                .withTokenHash(tokenCodec.hash(rawToken))
                .withExpiresAt(now.plusSeconds(3600))
                .build());

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(ResetPasswordWithTokenCommand.builder()
                        .token(rawToken)
                        .password("new-secure-password")
                        .repeatedPassword("different-password")
                        .build()))
                .isInstanceOf(PasswordsDoNotMatchException.class);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getPasswordHash())
                .isEqualTo("old-password");
    }
}
