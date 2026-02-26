package app.partsvibe.users.commands.usermanagement.password;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.RoleNames;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.errors.AdminPrivilegesRequiredException;
import app.partsvibe.users.errors.AdminReauthenticationFailedException;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

/*
 * Notice: Users IT context uses NoOpPasswordEncoder (from TestFakesConfiguration),
 * so test fixture password hashes intentionally equal raw passwords.
 */
class ResetUserPasswordByAdminCommandHandlerIT extends AbstractUsersIntegrationTest {
    private Role roleAdmin;
    private Role roleUser;
    private User authenticatedAdmin;

    @Autowired
    private ResetUserPasswordByAdminCommandHandler commandHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    protected void beforeEachTest(TestInfo testInfo) {
        roleAdmin = roleRepository
                .findByName(RoleNames.ADMIN)
                .orElseGet(() ->
                        roleRepository.save(aRole().withName(RoleNames.ADMIN).build()));
        roleUser = roleRepository
                .findByName(RoleNames.USER)
                .orElseGet(() ->
                        roleRepository.save(aRole().withName(RoleNames.USER).build()));
        authenticatedAdmin = userRepository.save(aUser().withUsername("admin@example.com")
                .withPasswordHash("admin-secret")
                .withRole(roleAdmin)
                .build());
        currentUserProvider.setCurrentUser(
                authenticatedAdmin.getId(), authenticatedAdmin.getUsername(), Set.of(RoleNames.ADMIN));
    }

    @Test
    void resetsTargetPasswordWhenAdminReauthenticatesSuccessfully() {
        // given
        User target = userRepository.save(aUser().withUsername("user@example.com")
                .withPasswordHash("old-target-password")
                .withRole(roleUser)
                .build());

        // when
        ResetUserPasswordByAdminCommandResult result = commandHandler.handle(ResetUserPasswordByAdminCommand.builder()
                .targetUserId(target.getId())
                .adminUserId(authenticatedAdmin.getId())
                .adminPassword("admin-secret")
                .build());

        // then
        assertThat(result.targetUserId()).isEqualTo(target.getId());
        assertThat(result.targetUsername()).isEqualTo("user@example.com");
        assertThat(result.temporaryPassword()).isNotBlank();

        User savedTarget = userRepository.findById(target.getId()).orElseThrow();
        assertThat(savedTarget.getPasswordHash()).isEqualTo(result.temporaryPassword());
        assertThat(savedTarget.getPasswordHash()).isNotEqualTo("old-target-password");
    }

    @Test
    void rejectsResetWhenAdminPasswordIsInvalid() {
        // given
        User target = userRepository.save(aUser().withUsername("user@example.com")
                .withPasswordHash("old-target-password")
                .withRole(roleUser)
                .build());

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(ResetUserPasswordByAdminCommand.builder()
                        .targetUserId(target.getId())
                        .adminUserId(authenticatedAdmin.getId())
                        .adminPassword("wrong-password")
                        .build()))
                .isInstanceOf(AdminReauthenticationFailedException.class);
        assertThat(userRepository.findById(target.getId()).orElseThrow().getPasswordHash())
                .isEqualTo("old-target-password");
    }

    @Test
    void rejectsResetWhenRequesterIsNotAdmin() {
        // given
        User requester = userRepository.save(aUser().withUsername("requester@example.com")
                .withPasswordHash("requester-secret")
                .withRole(roleUser)
                .build());
        User target = userRepository.save(aUser().withUsername("user@example.com")
                .withPasswordHash("old-target-password")
                .withRole(roleUser)
                .build());
        currentUserProvider.setCurrentUser(requester.getId(), requester.getUsername(), Set.of(RoleNames.USER));

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(ResetUserPasswordByAdminCommand.builder()
                        .targetUserId(target.getId())
                        .adminUserId(requester.getId())
                        .adminPassword("requester-secret")
                        .build()))
                .isInstanceOf(AdminPrivilegesRequiredException.class);
        assertThat(userRepository.findById(target.getId()).orElseThrow().getPasswordHash())
                .isEqualTo("old-target-password");
    }

    @Test
    void rejectsResetWhenCommandAdminDiffersFromAuthenticatedAdmin() {
        // given
        User otherAdmin = userRepository.save(aUser().withUsername("other-admin@example.com")
                .withPasswordHash("other-secret")
                .withRole(roleAdmin)
                .build());
        User target = userRepository.save(aUser().withUsername("user@example.com")
                .withPasswordHash("old-target-password")
                .withRole(roleUser)
                .build());
        // when / then
        assertThatThrownBy(() -> commandHandler.handle(ResetUserPasswordByAdminCommand.builder()
                        .targetUserId(target.getId())
                        .adminUserId(otherAdmin.getId())
                        .adminPassword("other-secret")
                        .build()))
                .isInstanceOf(AdminPrivilegesRequiredException.class);
        assertThat(userRepository.findById(target.getId()).orElseThrow().getPasswordHash())
                .isEqualTo("old-target-password");
    }
}
