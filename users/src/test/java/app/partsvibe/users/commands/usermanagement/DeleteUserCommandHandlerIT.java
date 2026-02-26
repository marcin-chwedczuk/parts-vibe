package app.partsvibe.users.commands.usermanagement;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserAvatarChangeRequestTestDataBuilder.aUserAvatarChangeRequest;
import static app.partsvibe.users.test.databuilders.UserPasswordResetTokenTestDataBuilder.aUserPasswordResetToken;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.RoleNames;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.avatar.UserAvatarChangeRequestRepository;
import app.partsvibe.users.repo.security.UserPasswordResetTokenRepository;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

class DeleteUserCommandHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private DeleteUserCommandHandler commandHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserPasswordResetTokenRepository tokenRepository;

    @Autowired
    private CredentialTokenCodec tokenCodec;

    @Autowired
    private UserAvatarChangeRequestRepository avatarChangeRequestRepository;

    @Override
    protected void beforeEachTest(TestInfo testInfo) {
        roleRepository
                .findByName(RoleNames.USER)
                .orElseGet(() ->
                        roleRepository.save(aRole().withName(RoleNames.USER).build()));
        roleRepository
                .findByName(RoleNames.ADMIN)
                .orElseGet(() ->
                        roleRepository.save(aRole().withName(RoleNames.ADMIN).build()));
        timeProvider.setNow(Instant.parse("2026-02-25T12:00:00Z"));
    }

    @Test
    void deletesRegularUserAndReturnsDeletedUsername() {
        currentUserProvider.setCurrentUser("admin-operator", Set.of(RoleNames.ADMIN));

        Role roleUser = roleRepository.findByName(RoleNames.USER).orElseThrow();
        User target = userRepository.save(
                aUser().withUsername("deletable-user").withRole(roleUser).build());

        DeleteUserCommandResult result = commandHandler.handle(new DeleteUserCommand(target.getId()));

        assertThat(result.deletedUsername()).isEqualTo("deletable-user");
        assertThat(userRepository.findById(target.getId())).isEmpty();
    }

    @Test
    void deleteIsIdempotentWhenUserDoesNotExist() {
        currentUserProvider.setCurrentUser("admin-operator", Set.of(RoleNames.ADMIN));

        DeleteUserCommandResult result = commandHandler.handle(new DeleteUserCommand(999_999L));

        assertThat(result.deletedUsername()).isNull();
    }

    @Test
    void rejectsDeletingCurrentUser() {
        Role roleAdmin = roleRepository.findByName(RoleNames.ADMIN).orElseThrow();
        User self = userRepository.save(
                aUser().withUsername("admin-self").enabled().withRole(roleAdmin).build());
        currentUserProvider.setCurrentUser(self.getId(), "admin-self", Set.of(RoleNames.ADMIN));

        assertThatThrownBy(() -> commandHandler.handle(new DeleteUserCommand(self.getId())))
                .isInstanceOf(CannotDeleteCurrentUserException.class);
        assertThat(userRepository.findById(self.getId())).isPresent();
    }

    @Test
    void rejectsDeletingLastActiveAdmin() {
        currentUserProvider.setCurrentUser("admin-operator", Set.of(RoleNames.ADMIN));

        Role roleAdmin = roleRepository.findByName(RoleNames.ADMIN).orElseThrow();
        User onlyAdmin = userRepository.save(
                aUser().withUsername("only-admin").enabled().withRole(roleAdmin).build());

        assertThatThrownBy(() -> commandHandler.handle(new DeleteUserCommand(onlyAdmin.getId())))
                .isInstanceOf(CannotDeleteLastActiveAdminException.class);
        assertThat(userRepository.findById(onlyAdmin.getId())).isPresent();
    }

    @Test
    void allowsDeletingActiveAdminWhenAnotherActiveAdminExists() {
        currentUserProvider.setCurrentUser("admin-operator", Set.of(RoleNames.ADMIN));

        Role roleAdmin = roleRepository.findByName(RoleNames.ADMIN).orElseThrow();
        userRepository.save(
                aUser().withUsername("admin-a").enabled().withRole(roleAdmin).build());
        User adminB = userRepository.save(
                aUser().withUsername("admin-b").enabled().withRole(roleAdmin).build());

        DeleteUserCommandResult result = commandHandler.handle(new DeleteUserCommand(adminB.getId()));

        assertThat(result.deletedUsername()).isEqualTo("admin-b");
        assertThat(userRepository.findById(adminB.getId())).isEmpty();
        assertThat(userRepository.countActiveUsersByRoleName(RoleNames.ADMIN)).isEqualTo(1);
    }

    @Test
    void deletesPasswordResetTokensAndAvatarChangeRequestsOwnedByDeletedUser() {
        // given
        currentUserProvider.setCurrentUser("admin-operator", Set.of(RoleNames.ADMIN));
        Role roleUser = roleRepository.findByName(RoleNames.USER).orElseThrow();
        User target = userRepository.save(aUser().withUsername("deletable-with-token@example.com")
                .withRole(roleUser)
                .build());
        tokenRepository.save(aUserPasswordResetToken()
                .withUser(target)
                .withTokenHash(tokenCodec.hash("deletable-with-token"))
                .withExpiresAt(timeProvider.now().plusSeconds(3600))
                .build());
        avatarChangeRequestRepository.save(aUserAvatarChangeRequest()
                .withUser(target)
                .withRequestedAt(timeProvider.now())
                .build());

        // when
        DeleteUserCommandResult result = commandHandler.handle(new DeleteUserCommand(target.getId()));

        // then
        assertThat(result.deletedUsername()).isEqualTo("deletable-with-token@example.com");
        assertThat(userRepository.findById(target.getId())).isEmpty();
        assertThat(tokenRepository.findAll().stream()
                        .filter(token -> token.getUser().getId().equals(target.getId())))
                .isEmpty();
        assertThat(avatarChangeRequestRepository.findAll().stream()
                        .filter(request -> request.getUser().getId().equals(target.getId())))
                .isEmpty();
    }
}
