package app.partsvibe.users.commands.usermanagement;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.RoleNames;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
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
}
