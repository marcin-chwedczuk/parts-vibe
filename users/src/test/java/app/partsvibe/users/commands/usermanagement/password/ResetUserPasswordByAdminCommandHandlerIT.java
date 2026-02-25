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
import org.springframework.beans.factory.annotation.Autowired;

class ResetUserPasswordByAdminCommandHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private ResetUserPasswordByAdminCommandHandler commandHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void resetsTargetPasswordWhenAdminReauthenticatesSuccessfully() {
        // given
        Role roleAdmin = roleRepository.save(aRole().withName(RoleNames.ADMIN).build());
        Role roleUser = roleRepository.save(aRole().withName(RoleNames.USER).build());

        User admin = userRepository.save(aUser().withUsername("admin@example.com")
                .withPasswordHash("admin-secret")
                .withRole(roleAdmin)
                .build());
        User target = userRepository.save(aUser().withUsername("user@example.com")
                .withPasswordHash("old-target-password")
                .withRole(roleUser)
                .build());
        currentUserProvider.setCurrentUser(admin.getId(), admin.getUsername(), Set.of(RoleNames.ADMIN));

        // when
        ResetUserPasswordByAdminCommandResult result = commandHandler.handle(
                new ResetUserPasswordByAdminCommand(target.getId(), admin.getId(), "admin-secret"));

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
        Role roleAdmin = roleRepository.save(aRole().withName(RoleNames.ADMIN).build());
        Role roleUser = roleRepository.save(aRole().withName(RoleNames.USER).build());

        User admin = userRepository.save(aUser().withUsername("admin@example.com")
                .withPasswordHash("admin-secret")
                .withRole(roleAdmin)
                .build());
        User target = userRepository.save(aUser().withUsername("user@example.com")
                .withPasswordHash("old-target-password")
                .withRole(roleUser)
                .build());
        currentUserProvider.setCurrentUser(admin.getId(), admin.getUsername(), Set.of(RoleNames.ADMIN));

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(
                        new ResetUserPasswordByAdminCommand(target.getId(), admin.getId(), "wrong-password")))
                .isInstanceOf(AdminReauthenticationFailedException.class);
        assertThat(userRepository.findById(target.getId()).orElseThrow().getPasswordHash())
                .isEqualTo("old-target-password");
    }

    @Test
    void rejectsResetWhenRequesterIsNotAdmin() {
        // given
        Role roleUser = roleRepository.save(aRole().withName(RoleNames.USER).build());

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
        assertThatThrownBy(() -> commandHandler.handle(
                        new ResetUserPasswordByAdminCommand(target.getId(), requester.getId(), "requester-secret")))
                .isInstanceOf(AdminPrivilegesRequiredException.class);
        assertThat(userRepository.findById(target.getId()).orElseThrow().getPasswordHash())
                .isEqualTo("old-target-password");
    }

    @Test
    void rejectsResetWhenCommandAdminDiffersFromAuthenticatedAdmin() {
        // given
        Role roleAdmin = roleRepository.save(aRole().withName(RoleNames.ADMIN).build());
        Role roleUser = roleRepository.save(aRole().withName(RoleNames.USER).build());

        User authenticatedAdmin = userRepository.save(aUser().withUsername("admin@example.com")
                .withPasswordHash("admin-secret")
                .withRole(roleAdmin)
                .build());
        User otherAdmin = userRepository.save(aUser().withUsername("other-admin@example.com")
                .withPasswordHash("other-secret")
                .withRole(roleAdmin)
                .build());
        User target = userRepository.save(aUser().withUsername("user@example.com")
                .withPasswordHash("old-target-password")
                .withRole(roleUser)
                .build());
        currentUserProvider.setCurrentUser(
                authenticatedAdmin.getId(), authenticatedAdmin.getUsername(), Set.of(RoleNames.ADMIN));

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(
                        new ResetUserPasswordByAdminCommand(target.getId(), otherAdmin.getId(), "other-secret")))
                .isInstanceOf(AdminPrivilegesRequiredException.class);
        assertThat(userRepository.findById(target.getId()).orElseThrow().getPasswordHash())
                .isEqualTo("old-target-password");
    }
}
