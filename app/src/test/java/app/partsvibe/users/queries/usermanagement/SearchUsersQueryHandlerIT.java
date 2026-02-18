package app.partsvibe.users.queries.usermanagement;

import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.shared.cqrs.PageResult;
import app.partsvibe.support.AbstractPostgresIntegrationTest;
import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class SearchUsersQueryHandlerIT extends AbstractPostgresIntegrationTest {
    @Autowired
    private SearchUsersQueryHandler queryHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void usernameContainsAndEnabledIsFiltersTogether() {
        Role role = roleRepository.save(new Role("ROLE_IT_FILTER"));
        saveUser("it-filter-alpha", true, role);
        saveUser("it-filter-beta", false, role);
        saveUser("other-user", true, role);

        SearchUsersQuery query = SearchUsersQuery.builder()
                .usernameContains(" FILTER-AL ")
                .enabledIs(true)
                .rolesContainAll(List.of("ROLE_IT_FILTER"))
                .currentPage(1)
                .pageSize(10)
                .sortBy(SearchUsersQuery.SORT_BY_USERNAME)
                .sortDir(SearchUsersQuery.SORT_ASC)
                .build();

        PageResult<SearchUsersQuery.UserRow> result = queryHandler.handle(query);

        assertThat(result.items())
                .extracting(SearchUsersQuery.UserRow::username)
                .containsExactly("it-filter-alpha");
        assertThat(result.items()).allMatch(SearchUsersQuery.UserRow::enabled);
    }

    @Test
    void rolesContainAllRequiresEverySelectedRole() {
        Role alpha = roleRepository.save(new Role("ROLE_IT_ALPHA"));
        Role beta = roleRepository.save(new Role("ROLE_IT_BETA"));

        saveUser("it-role-both", true, alpha, beta);
        saveUser("it-role-alpha-only", true, alpha);
        saveUser("it-role-beta-only", true, beta);

        SearchUsersQuery query = SearchUsersQuery.builder()
                .usernameContains("it-role")
                .enabledIs(null)
                .rolesContainAll(List.of("ROLE_IT_ALPHA", "ROLE_IT_BETA"))
                .currentPage(1)
                .pageSize(10)
                .sortBy(SearchUsersQuery.SORT_BY_USERNAME)
                .sortDir(SearchUsersQuery.SORT_ASC)
                .build();

        PageResult<SearchUsersQuery.UserRow> result = queryHandler.handle(query);

        assertThat(result.items())
                .extracting(SearchUsersQuery.UserRow::username)
                .containsExactly("it-role-both");
    }

    private void saveUser(String username, boolean enabled, Role... roles) {
        User user = new User(username, "noop");
        user.setEnabled(enabled);
        user.getRoles().addAll(List.of(roles));
        userRepository.save(user);
    }
}
