package app.partsvibe.users.commands.invite;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.domain.invite.UserInvite;
import app.partsvibe.users.errors.InvalidInviteRoleException;
import app.partsvibe.users.events.UserInvitedEvent;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.invite.UserInviteRepository;
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
    private UserInviteRepository userInviteRepository;

    @Autowired
    private CredentialTokenCodec tokenCodec;

    @Autowired
    private EntityManager entityManager;

    @Test
    void createsInviteAndSendsEmailWhenUserDoesNotExist() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);
        roleRepository.save(aRole().withName("ROLE_USER").build());

        // when
        InviteUserCommandResult result =
                commandHandler.handle(new InviteUserCommand("New@Example.com", "role_user", 24, "Welcome aboard"));

        // then
        assertThat(result.outcome()).isEqualTo(InviteUserCommandResult.InviteOutcome.INVITE_SENT);
        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.expiresAt()).isEqualTo(now.plusSeconds(24 * 3600L));
        assertThat(userRepository.findByUsernameIgnoreCase("new@example.com")).isEmpty();

        var invites = userInviteRepository.findAll().stream()
                .filter(invite -> invite.getEmail().equals("new@example.com"))
                .toList();
        assertThat(invites).hasSize(1);
        assertThat(invites.get(0).getRoleName()).isEqualTo("ROLE_USER");
        assertThat(invites.get(0).getExpiresAt()).isEqualTo(now.plusSeconds(24 * 3600L));
        assertThat(invites.get(0).getRevokedAt()).isNull();

        assertThat(eventPublisher.publishedEvents()).hasSize(1);
        assertThat(eventPublisher.publishedEvents().get(0)).isInstanceOf(UserInvitedEvent.class);
        UserInvitedEvent event =
                (UserInvitedEvent) eventPublisher.publishedEvents().get(0);
        assertThat(event.email()).isEqualTo("new@example.com");
        assertThat(event.invitedRole()).isEqualTo("ROLE_USER");
        assertThat(event.inviteMessage()).isEqualTo("Welcome aboard");
        assertThat(tokenCodec.hash(event.token())).isEqualTo(invites.get(0).getTokenHash());
    }

    @Test
    void resendsInviteWhenEmailHasPendingInvite() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);
        roleRepository.save(aRole().withName("ROLE_USER").build());

        UserInvite previousInvite = userInviteRepository.save(new UserInvite(
                "invited@example.com", "ROLE_ADMIN", null, tokenCodec.hash("old-invite-token"), now.plusSeconds(3600)));

        // when
        InviteUserCommandResult result =
                commandHandler.handle(new InviteUserCommand("invited@example.com", "ROLE_USER", 24, null));

        // then
        entityManager.flush();
        entityManager.clear();

        assertThat(result.outcome()).isEqualTo(InviteUserCommandResult.InviteOutcome.INVITE_RESENT);

        UserInvite previousSaved =
                userInviteRepository.findById(previousInvite.getId()).orElseThrow();
        assertThat(previousSaved.getRevokedAt()).isEqualTo(now);

        var invites = userInviteRepository.findAll().stream()
                .filter(invite -> invite.getEmail().equals("invited@example.com"))
                .toList();
        assertThat(invites).hasSize(2);
        assertThat(invites.stream()
                        .filter(invite -> !invite.getId().equals(previousInvite.getId()))
                        .findFirst()
                        .orElseThrow()
                        .getRoleName())
                .isEqualTo("ROLE_USER");
        assertThat(invites.stream()
                        .filter(invite -> !invite.getId().equals(previousInvite.getId()))
                        .findFirst()
                        .orElseThrow()
                        .getRevokedAt())
                .isNull();
        assertThat(userRepository.findByUsernameIgnoreCase("invited@example.com"))
                .isEmpty();

        assertThat(eventPublisher.publishedEvents()).hasSize(1);
        assertThat(eventPublisher.publishedEvents().get(0)).isInstanceOf(UserInvitedEvent.class);
    }

    @Test
    void resendInviteRevokesAllOutstandingUnusedInviteTokens() {
        // given
        Instant now = Instant.parse("2026-02-25T12:00:00Z");
        timeProvider.setNow(now);
        roleRepository.save(aRole().withName("ROLE_USER").build());

        UserInvite firstOldInvite = userInviteRepository.save(new UserInvite(
                "multiple-tokens@example.com",
                "ROLE_USER",
                null,
                tokenCodec.hash("old-invite-token-1"),
                now.plusSeconds(3600)));
        UserInvite secondOldInvite = userInviteRepository.save(new UserInvite(
                "multiple-tokens@example.com",
                "ROLE_USER",
                null,
                tokenCodec.hash("old-invite-token-2"),
                now.plusSeconds(7200)));

        // when
        commandHandler.handle(new InviteUserCommand("multiple-tokens@example.com", "ROLE_USER", 24, null));

        // then
        entityManager.flush();
        entityManager.clear();

        UserInvite firstOldSaved =
                userInviteRepository.findById(firstOldInvite.getId()).orElseThrow();
        UserInvite secondOldSaved =
                userInviteRepository.findById(secondOldInvite.getId()).orElseThrow();
        assertThat(firstOldSaved.getRevokedAt()).isEqualTo(now);
        assertThat(secondOldSaved.getRevokedAt()).isEqualTo(now);

        var invites = userInviteRepository.findAll().stream()
                .filter(invite -> invite.getEmail().equals("multiple-tokens@example.com"))
                .toList();
        assertThat(invites).hasSize(3);
        assertThat(invites.stream().filter(invite -> invite.getRevokedAt() == null))
                .hasSize(1);
    }

    @Test
    void returnsAlreadyOnboardedWhenUserExists() {
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
        assertThat(userInviteRepository.findAll()).isEmpty();
        assertThat(eventPublisher.publishedEvents()).isEmpty();
        assertThat(userRepository.findById(user.getId())).isPresent();
    }

    @Test
    void returnsAlreadyOnboardedLockedWhenUserExistsAndIsLocked() {
        // given
        Role roleUser = roleRepository.save(aRole().withName("ROLE_USER").build());
        User user = userRepository.save(aUser().withUsername("locked@example.com")
                .withPasswordHash("password")
                .withRole(roleUser)
                .disabled()
                .build());

        // when
        InviteUserCommandResult result =
                commandHandler.handle(new InviteUserCommand("locked@example.com", "ROLE_USER", 24, null));

        // then
        assertThat(result.outcome()).isEqualTo(InviteUserCommandResult.InviteOutcome.ALREADY_ONBOARDED_LOCKED);
        assertThat(userInviteRepository.findAll()).isEmpty();
        assertThat(eventPublisher.publishedEvents()).isEmpty();
        assertThat(userRepository.findById(user.getId())).isPresent();
    }

    @Test
    void rejectsInviteWhenRoleDoesNotExistInDatabase() {
        // given
        timeProvider.setNow(Instant.parse("2026-02-25T12:00:00Z"));

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(
                        new InviteUserCommand("missing-role@example.com", "ROLE_UNKNOWN", 24, null)))
                .isInstanceOf(InvalidInviteRoleException.class);
    }
}
