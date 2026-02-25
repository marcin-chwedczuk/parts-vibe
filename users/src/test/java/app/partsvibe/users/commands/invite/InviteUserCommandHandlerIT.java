package app.partsvibe.users.commands.invite;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.domain.security.UserCredentialToken;
import app.partsvibe.users.domain.security.UserCredentialTokenPurpose;
import app.partsvibe.users.events.UserInvitedEvent;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.security.UserCredentialTokenRepository;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InviteUserCommandHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private InviteUserCommandHandler commandHandler;

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
    void createsUserAndSendsInviteWhenUserDoesNotExist() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);

        // when
        InviteUserCommandResult result =
                commandHandler.handle(new InviteUserCommand("New@Example.com", "role_user", 24, "Welcome aboard"));

        // then
        assertThat(result.outcome()).isEqualTo(InviteUserCommandResult.InviteOutcome.INVITE_SENT);
        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.expiresAt()).isEqualTo(now.plusSeconds(24 * 3600L));

        User savedUser = userRepository.findById(result.userId()).orElseThrow();
        assertThat(savedUser.getUsername()).isEqualTo("new@example.com");
        assertThat(savedUser.getRoles()).extracting(Role::getName).containsExactly("ROLE_USER");

        var inviteTokens = tokenRepository.findAll().stream()
                .filter(token -> token.getUser().getId().equals(savedUser.getId()))
                .filter(token -> token.getPurpose() == UserCredentialTokenPurpose.INVITE_ACTIVATION)
                .toList();
        assertThat(inviteTokens).hasSize(1);
        assertThat(inviteTokens.get(0).getExpiresAt()).isEqualTo(now.plusSeconds(24 * 3600L));

        assertThat(eventPublisher.publishedEvents()).hasSize(1);
        assertThat(eventPublisher.publishedEvents().get(0)).isInstanceOf(UserInvitedEvent.class);
        UserInvitedEvent event =
                (UserInvitedEvent) eventPublisher.publishedEvents().get(0);
        assertThat(event.email()).isEqualTo("new@example.com");
        assertThat(event.invitedRole()).isEqualTo("ROLE_USER");
        assertThat(event.inviteMessage()).isEqualTo("Welcome aboard");
        assertThat(tokenCodec.hash(event.token())).isEqualTo(inviteTokens.get(0).getTokenHash());
    }

    @Test
    void resendsInviteWhenUserHasPendingInvite() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);

        Role roleUser = roleRepository.save(aRole().withName("ROLE_USER").build());
        User user = userRepository.save(aUser().withUsername("invited@example.com")
                .withPasswordHash("placeholder-password")
                .withRole(roleUser)
                .build());

        UserCredentialToken previousInvite = tokenRepository.save(new UserCredentialToken(
                user,
                tokenCodec.hash("old-invite-token"),
                UserCredentialTokenPurpose.INVITE_ACTIVATION,
                now.plusSeconds(3600)));

        // when
        InviteUserCommandResult result =
                commandHandler.handle(new InviteUserCommand("invited@example.com", "ROLE_USER", 24, null));

        // then
        entityManager.flush();
        entityManager.clear();

        assertThat(result.outcome()).isEqualTo(InviteUserCommandResult.InviteOutcome.INVITE_RESENT);

        UserCredentialToken previousSaved =
                tokenRepository.findById(previousInvite.getId()).orElseThrow();
        assertThat(previousSaved.getRevokedAt()).isEqualTo(now);

        var inviteTokens = tokenRepository.findAll().stream()
                .filter(token -> token.getUser().getId().equals(user.getId()))
                .filter(token -> token.getPurpose() == UserCredentialTokenPurpose.INVITE_ACTIVATION)
                .toList();
        assertThat(inviteTokens).hasSize(2);
        assertThat(inviteTokens.stream()
                        .filter(token -> !token.getId().equals(previousInvite.getId()))
                        .findFirst()
                        .orElseThrow()
                        .getRevokedAt())
                .isNull();

        assertThat(eventPublisher.publishedEvents()).hasSize(1);
        assertThat(eventPublisher.publishedEvents().get(0)).isInstanceOf(UserInvitedEvent.class);
    }

    @Test
    void returnsAlreadyOnboardedWhenUserHasNoInviteHistory() {
        // given
        Role roleUser = roleRepository.save(aRole().withName("ROLE_USER").build());
        User user = userRepository.save(aUser().withUsername("active@example.com")
                .withPasswordHash("password")
                .withRole(roleUser)
                .build());

        // when
        InviteUserCommandResult result =
                commandHandler.handle(new InviteUserCommand("active@example.com", "ROLE_USER", 24, null));

        // then
        assertThat(result.outcome()).isEqualTo(InviteUserCommandResult.InviteOutcome.ALREADY_ONBOARDED);
        assertThat(tokenRepository.findAll()).isEmpty();
        assertThat(eventPublisher.publishedEvents()).isEmpty();
        assertThat(userRepository.findById(user.getId())).isPresent();
    }
}
