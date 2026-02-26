package app.partsvibe.users.commands.password;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserPasswordResetTokenTestDataBuilder.aUserPasswordResetToken;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.RoleNames;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.domain.security.UserPasswordResetToken;
import app.partsvibe.users.events.PasswordResetRequestedEvent;
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

class RequestPasswordResetCommandHandlerIT extends AbstractUsersIntegrationTest {
    private static final Instant NOW_2026_02_25T12_00Z = Instant.parse("2026-02-25T12:00:00Z");

    @Autowired
    private RequestPasswordResetCommandHandler commandHandler;

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
    void createsNewPasswordResetTokenRevokesPreviousActiveAndPublishesEvent() {
        // given
        Instant now = NOW_2026_02_25T12_00Z;

        Role roleUser = roleRepository.findByName(RoleNames.USER).orElseThrow();
        User user = userRepository.save(aUser().withUsername("alice@example.com")
                .withPasswordHash("old-password")
                .withRole(roleUser)
                .build());

        UserPasswordResetToken previousToken = tokenRepository.save(aUserPasswordResetToken()
                .withUser(user)
                .withTokenHash(tokenCodec.hash("previous-reset-token"))
                .withExpiresAt(now.plusSeconds(1800))
                .build());

        // when
        commandHandler.handle(new RequestPasswordResetCommand("  ALICE@Example.com "));

        // then
        entityManager.flush();
        entityManager.clear();

        var passwordResetTokens = tokenRepository.findAll().stream()
                .filter(token -> token.getUser().getId().equals(user.getId()))
                .toList();
        assertThat(passwordResetTokens).hasSize(2);

        UserPasswordResetToken revokedToken = passwordResetTokens.stream()
                .filter(token -> token.getId().equals(previousToken.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(revokedToken.getRevokedAt()).isEqualTo(now);

        UserPasswordResetToken activeToken = passwordResetTokens.stream()
                .filter(token -> !token.getId().equals(previousToken.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(activeToken.getRevokedAt()).isNull();
        assertThat(activeToken.getUsedAt()).isNull();
        assertThat(activeToken.getExpiresAt()).isEqualTo(now.plusSeconds(24 * 3600L));

        assertThat(eventPublisher.publishedEvents())
                .singleElement()
                .isInstanceOfSatisfying(PasswordResetRequestedEvent.class, event -> {
                    assertThat(event.email()).isEqualTo("alice@example.com");
                    assertThat(event.expiresAt()).isEqualTo(now.plusSeconds(24 * 3600L));
                    assertThat(tokenCodec.hash(event.token())).isEqualTo(activeToken.getTokenHash());
                });
    }

    @Test
    void doesNothingWhenUserDoesNotExist() {
        // given
        // when
        commandHandler.handle(new RequestPasswordResetCommand("missing@example.com"));

        // then
        assertThat(tokenRepository.findAll()).isEmpty();
        assertThat(eventPublisher.publishedEvents()).isEmpty();
    }
}
