package app.partsvibe.users.commands.usermanagement;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.testsupport.it.AbstractUsersIntegrationTest;
import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.errors.UsernameAlreadyExistsException;
import app.partsvibe.users.models.UserDetailsModel;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CreateUserCommandHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private CreateUserCommandHandler commandHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void createsUserWithCanonicalUsernameAndDefaultRoleUser() {
        UserDetailsModel result = commandHandler.handle(new CreateUserCommand("  Bob@Example.COM  ", true));

        assertThat(result.id()).isNotNull();
        assertThat(result.username()).isEqualTo("bob@example.com");
        assertThat(result.enabled()).isTrue();
        assertThat(result.roles()).containsExactly("ROLE_USER");

        User saved = userRepository.findById(result.id()).orElseThrow();
        assertThat(saved.getUsername()).isEqualTo("bob@example.com");
        assertThat(saved.getPasswordHash()).isEqualTo("{noop}PENDING_ACTIVATION");
        assertThat(saved.getRoles()).extracting(Role::getName).containsExactly("ROLE_USER");
    }

    @Test
    void rejectsDuplicateUsernameCaseInsensitive() {
        roleRepository.save(aRole().withName("ROLE_USER").build());
        userRepository.save(aUser().withUsername("alice@example.com")
                .withPasswordHash("{noop}x")
                .enabled()
                .build());

        assertThatThrownBy(() -> commandHandler.handle(new CreateUserCommand("ALICE@example.com", true)))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }
}
