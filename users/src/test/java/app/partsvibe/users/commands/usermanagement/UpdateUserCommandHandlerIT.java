package app.partsvibe.users.commands.usermanagement;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.RoleNames;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.errors.UserNotFoundException;
import app.partsvibe.users.errors.UsernameAlreadyExistsException;
import app.partsvibe.users.models.UserDetailsModel;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

class UpdateUserCommandHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private UpdateUserCommandHandler commandHandler;

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
    }

    @Test
    void updatesUsernameAndEnabledWithCanonicalUsername() {
        Role roleUser = roleRepository.findByName(RoleNames.USER).orElseThrow();
        User target = userRepository.save(aUser().withUsername("before@example.com")
                .withPasswordHash("{noop}x")
                .enabled()
                .withRole(roleUser)
                .build());

        UserDetailsModel result = commandHandler.handle(UpdateUserCommand.builder()
                .userId(target.getId())
                .username(" After@Example.COM ")
                .enabled(false)
                .build());

        assertThat(result.username()).isEqualTo("after@example.com");
        assertThat(result.enabled()).isFalse();

        User saved = userRepository.findById(target.getId()).orElseThrow();
        assertThat(saved.getUsername()).isEqualTo("after@example.com");
        assertThat(saved.isEnabled()).isFalse();
    }

    @Test
    void rejectsDuplicateUsernameCaseInsensitive() {
        Role roleUser = roleRepository.findByName(RoleNames.USER).orElseThrow();
        userRepository.save(aUser().withUsername("existing@example.com")
                .withPasswordHash("{noop}x")
                .enabled()
                .withRole(roleUser)
                .build());
        User target = userRepository.save(aUser().withUsername("other@example.com")
                .withPasswordHash("{noop}x")
                .enabled()
                .withRole(roleUser)
                .build());

        assertThatThrownBy(() -> commandHandler.handle(UpdateUserCommand.builder()
                        .userId(target.getId())
                        .username("EXISTING@example.com")
                        .enabled(true)
                        .build()))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    void throwsWhenUserNotFound() {
        assertThatThrownBy(() -> commandHandler.handle(UpdateUserCommand.builder()
                        .userId(999_999L)
                        .username("x@example.com")
                        .enabled(true)
                        .build()))
                .isInstanceOf(UserNotFoundException.class);
    }
}
