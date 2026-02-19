package app.partsvibe.users.queries.auth;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.users.domain.Role;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.testsupport.AbstractUsersIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@Import(FindUserDetailsByUsernameQueryHandler.class)
class FindUserDetailsByUsernameQueryHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private FindUserDetailsByUsernameQueryHandler queryHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void returnsUserDetailsWithAuthoritiesWhenUserExists() {
        Role roleUser =
                roleRepository.save(aRole().withName("ROLE_IT_AUTH_USER").build());
        Role roleAdmin =
                roleRepository.save(aRole().withName("ROLE_IT_AUTH_ADMIN").build());
        userRepository.save(aUser().withUsername("it-auth-jane")
                .withPasswordHash("{noop}it-secret")
                .enabled()
                .withRoles(roleUser, roleAdmin)
                .build());

        UserDetails result = queryHandler.handle(new FindUserDetailsByUsernameQuery("it-auth-jane"));

        assertThat(result.getUsername()).isEqualTo("it-auth-jane");
        assertThat(result.getPassword()).isEqualTo("{noop}it-secret");
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder("ROLE_IT_AUTH_USER", "ROLE_IT_AUTH_ADMIN");
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        assertThatThrownBy(() -> queryHandler.handle(new FindUserDetailsByUsernameQuery("missing-user")))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found: missing-user");
    }
}
