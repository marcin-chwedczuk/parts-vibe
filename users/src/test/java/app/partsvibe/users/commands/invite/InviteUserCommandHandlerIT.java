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
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

class InviteUserCommandHandlerIT extends AbstractUsersIntegrationTest {
    private static final Instant NOW_2026_02_25T12_00Z = Instant.parse("2026-02-25T12:00:00Z");

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

    @Override
    protected void beforeEachTest(TestInfo testInfo) {
        timeProvider.setNow(NOW_2026_02_25T12_00Z);
        roleRepository
                .findByName(RoleNames.USER)
                .orElseGet(() ->
                        roleRepository.save(aRole().withName(RoleNames.USER).build()));
    }

    @Test
    void createsInviteAndSendsEmailWhenUserDoesNotExist() {
        // given
        Instant now = NOW_2026_02_25T12_00Z;

        // when
        InviteUserCommandResult result = commandHandler.handle(InviteUserCommand.builder()
                .email("New@Example.com")
                .roleName("role_user")
                .validityHours(24)
                .inviteMessage("Welcome aboard")
                .build());

        // then
        assertThat(result.outcome()).isEqualTo(InviteUserCommandResult.InviteOutcome.INVITE_SENT);
        assertThat(result.email()).isEqualTo("new@example.com");
        assertThat(result.expiresAt()).isEqualTo(now.plusSeconds(24 * 3600L));
        assertThat(userRepository.findByUsernameIgnoreCase("new@example.com")).isEmpty();

        var invites = userInviteRepository.findAll().stream()
                .filter(invite -> invite.getEmail().equals("new@example.com"))
                .toList();
        assertThat(invites).hasSize(1);
        assertThat(invites.getFirst().getRoleName()).isEqualTo(RoleNames.USER);
        assertThat(invites.getFirst().getExpiresAt()).isEqualTo(now.plusSeconds(24 * 3600L));
        assertThat(invites.getFirst().getRevokedAt()).isNull();

        assertThat(eventPublisher.publishedEvents())
                .singleElement()
                .isInstanceOfSatisfying(UserInvitedEvent.class, event -> {
                    assertThat(event.email()).isEqualTo("new@example.com");
                    assertThat(event.invitedRole()).isEqualTo(RoleNames.USER);
                    assertThat(event.inviteMessage()).isEqualTo("Welcome aboard");
                    assertThat(tokenCodec.hash(event.token()))
                            .isEqualTo(invites.getFirst().getTokenHash());
                });
    }

    @Test
    void resendsInviteWhenEmailHasPendingInvite() {
        // given
        Instant now = NOW_2026_02_25T12_00Z;

        UserInvite previousInvite = userInviteRepository.save(aUserInvite()
                .withEmail("invited@example.com")
                .withRoleName(RoleNames.ADMIN)
                .withTokenHash(tokenCodec.hash("old-invite-token"))
                .withExpiresAt(now.plusSeconds(3600))
                .build());

        // when
        InviteUserCommandResult result = commandHandler.handle(InviteUserCommand.builder()
                .email("invited@example.com")
                .roleName(RoleNames.USER)
                .validityHours(24)
                .inviteMessage(null)
                .build());

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
                .isEqualTo(RoleNames.USER);
        assertThat(invites.stream()
                        .filter(invite -> !invite.getId().equals(previousInvite.getId()))
                        .findFirst()
                        .orElseThrow()
                        .getRevokedAt())
                .isNull();
        assertThat(userRepository.findByUsernameIgnoreCase("invited@example.com"))
                .isEmpty();

        assertThat(eventPublisher.publishedEvents()).singleElement().isInstanceOf(UserInvitedEvent.class);
    }

    @Test
    void resendInviteRevokesAllOutstandingUnusedInviteTokens() {
        // given
        Instant now = NOW_2026_02_25T12_00Z;

        UserInvite firstOldInvite = userInviteRepository.save(aUserInvite()
                .withEmail("multiple-tokens@example.com")
                .withRoleName(RoleNames.USER)
                .withTokenHash(tokenCodec.hash("old-invite-token-1"))
                .withExpiresAt(now.plusSeconds(3600))
                .build());
        UserInvite secondOldInvite = userInviteRepository.save(aUserInvite()
                .withEmail("multiple-tokens@example.com")
                .withRoleName(RoleNames.USER)
                .withTokenHash(tokenCodec.hash("old-invite-token-2"))
                .withExpiresAt(now.plusSeconds(7200))
                .build());

        // when
        commandHandler.handle(InviteUserCommand.builder()
                .email("multiple-tokens@example.com")
                .roleName(RoleNames.USER)
                .validityHours(24)
                .inviteMessage(null)
                .build());

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
        Role roleUser = roleRepository.findByName(RoleNames.USER).orElseThrow();
        User user = userRepository.save(aUser().withUsername("active@example.com")
                .withPasswordHash("password")
                .withRole(roleUser)
                .build());

        // when
        InviteUserCommandResult result = commandHandler.handle(InviteUserCommand.builder()
                .email("active@example.com")
                .roleName(RoleNames.USER)
                .validityHours(24)
                .inviteMessage(null)
                .build());

        // then
        assertThat(result.outcome()).isEqualTo(InviteUserCommandResult.InviteOutcome.ALREADY_ONBOARDED);
        assertThat(userInviteRepository.findAll()).isEmpty();
        assertThat(eventPublisher.publishedEvents()).isEmpty();
        assertThat(userRepository.findById(user.getId())).isPresent();
    }

    @Test
    void returnsAlreadyOnboardedLockedWhenUserExistsAndIsLocked() {
        // given
        Role roleUser = roleRepository.findByName(RoleNames.USER).orElseThrow();
        User user = userRepository.save(aUser().withUsername("locked@example.com")
                .withPasswordHash("password")
                .withRole(roleUser)
                .disabled()
                .build());

        // when
        InviteUserCommandResult result = commandHandler.handle(InviteUserCommand.builder()
                .email("locked@example.com")
                .roleName(RoleNames.USER)
                .validityHours(24)
                .inviteMessage(null)
                .build());

        // then
        assertThat(result.outcome()).isEqualTo(InviteUserCommandResult.InviteOutcome.ALREADY_ONBOARDED_LOCKED);
        assertThat(userInviteRepository.findAll()).isEmpty();
        assertThat(eventPublisher.publishedEvents()).isEmpty();
        assertThat(userRepository.findById(user.getId())).isPresent();
    }

    @Test
    void rejectsInviteWhenRoleDoesNotExistInDatabase() {
        // given
        // when / then
        assertThatThrownBy(() -> commandHandler.handle(InviteUserCommand.builder()
                        .email("missing-role@example.com")
                        .roleName("ROLE_UNKNOWN")
                        .validityHours(24)
                        .inviteMessage(null)
                        .build()))
                .isInstanceOf(InvalidInviteRoleException.class);
    }
}
