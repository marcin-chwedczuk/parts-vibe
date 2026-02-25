package app.partsvibe.users.queries.usermanagement;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.users.errors.UserNotFoundException;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserByIdQueryHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private UserByIdQueryHandler queryHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void returnsUserDetailsWithSortedRoles() {
        // given
        var roleB = roleRepository.save(aRole().withName("ROLE_ZETA").build());
        var roleA = roleRepository.save(aRole().withName("ROLE_ALPHA").build());
        var user = userRepository.save(aUser().withUsername("details-user@example.com")
                .enabled()
                .withRoles(roleB, roleA)
                .build());

        // when
        var result = queryHandler.handle(new UserByIdQuery(user.getId()));

        // then
        assertThat(result.id()).isEqualTo(user.getId());
        assertThat(result.username()).isEqualTo("details-user@example.com");
        assertThat(result.enabled()).isTrue();
        assertThat(result.roles()).containsExactly("ROLE_ALPHA", "ROLE_ZETA");
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        // given / when / then
        assertThatThrownBy(() -> queryHandler.handle(new UserByIdQuery(999_999L)))
                .isInstanceOf(UserNotFoundException.class);
    }
}
