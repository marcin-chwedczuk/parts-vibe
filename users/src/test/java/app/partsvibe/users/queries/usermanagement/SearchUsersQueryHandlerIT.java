package app.partsvibe.users.queries.usermanagement;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.shared.cqrs.PageResult;
import app.partsvibe.testsupport.it.AbstractUsersIntegrationTest;
import app.partsvibe.users.domain.Role;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SearchUsersQueryHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private SearchUsersQueryHandler queryHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void usernameContainsAndEnabledIsFiltersTogether() {
        Role role = roleRepository.save(aRole().withName("ROLE_IT_FILTER").build());
        userRepository.save(
                aUser().withUsername("it-filter-alpha").enabled().withRole(role).build());
        userRepository.save(
                aUser().withUsername("it-filter-beta").disabled().withRole(role).build());
        userRepository.save(
                aUser().withUsername("other-user").enabled().withRole(role).build());

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
        Role alpha = roleRepository.save(aRole().withName("ROLE_IT_ALPHA").build());
        Role beta = roleRepository.save(aRole().withName("ROLE_IT_BETA").build());

        userRepository.save(aUser().withUsername("it-role-both")
                .enabled()
                .withRoles(alpha, beta)
                .build());
        userRepository.save(aUser().withUsername("it-role-alpha-only")
                .enabled()
                .withRole(alpha)
                .build());
        userRepository.save(aUser().withUsername("it-role-beta-only")
                .enabled()
                .withRole(beta)
                .build());

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
}
