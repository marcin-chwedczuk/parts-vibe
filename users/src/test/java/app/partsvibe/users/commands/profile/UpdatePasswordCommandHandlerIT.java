package app.partsvibe.users.commands.profile;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.errors.CurrentUserMismatchException;
import app.partsvibe.users.errors.InvalidCurrentPasswordException;
import app.partsvibe.users.errors.PasswordsDoNotMatchException;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UpdatePasswordCommandHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private UpdatePasswordCommandHandler commandHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void changesPasswordWhenCurrentPasswordIsValid() {
        // given
        Role roleUser = roleRepository.save(aRole().withName("ROLE_USER").build());
        User user = userRepository.save(aUser().withUsername("alice@example.com")
                .withPasswordHash("current-password")
                .withRole(roleUser)
                .build());
        currentUserProvider.setCurrentUser(user.getId(), user.getUsername(), Set.of("ROLE_USER"));

        // when
        commandHandler.handle(new UpdatePasswordCommand(
                user.getId(), "current-password", "new-secure-password", "new-secure-password"));

        // then
        User saved = userRepository.findById(user.getId()).orElseThrow();
        assertThat(saved.getPasswordHash()).isEqualTo("new-secure-password");
    }

    @Test
    void rejectsChangeWhenCurrentPasswordIsInvalid() {
        // given
        Role roleUser = roleRepository.save(aRole().withName("ROLE_USER").build());
        User user = userRepository.save(aUser().withUsername("alice@example.com")
                .withPasswordHash("current-password")
                .withRole(roleUser)
                .build());
        currentUserProvider.setCurrentUser(user.getId(), user.getUsername(), Set.of("ROLE_USER"));

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(new UpdatePasswordCommand(
                        user.getId(), "wrong-password", "new-secure-password", "new-secure-password")))
                .isInstanceOf(InvalidCurrentPasswordException.class);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getPasswordHash())
                .isEqualTo("current-password");
    }

    @Test
    void rejectsChangeWhenPasswordsDoNotMatch() {
        // given
        Role roleUser = roleRepository.save(aRole().withName("ROLE_USER").build());
        User user = userRepository.save(aUser().withUsername("alice@example.com")
                .withPasswordHash("current-password")
                .withRole(roleUser)
                .build());
        currentUserProvider.setCurrentUser(user.getId(), user.getUsername(), Set.of("ROLE_USER"));

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(new UpdatePasswordCommand(
                        user.getId(), "current-password", "new-secure-password", "different-password")))
                .isInstanceOf(PasswordsDoNotMatchException.class);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getPasswordHash())
                .isEqualTo("current-password");
    }

    @Test
    void rejectsChangeWhenAuthenticatedUserDiffersFromCommandUser() {
        // given
        Role roleUser = roleRepository.save(aRole().withName("ROLE_USER").build());
        User user = userRepository.save(aUser().withUsername("alice@example.com")
                .withPasswordHash("current-password")
                .withRole(roleUser)
                .build());
        currentUserProvider.setCurrentUser(999L, "other@example.com", Set.of("ROLE_USER"));

        // when / then
        assertThatThrownBy(() -> commandHandler.handle(new UpdatePasswordCommand(
                        user.getId(), "current-password", "new-secure-password", "new-secure-password")))
                .isInstanceOf(CurrentUserMismatchException.class);
        assertThat(userRepository.findById(user.getId()).orElseThrow().getPasswordHash())
                .isEqualTo("current-password");
    }
}
