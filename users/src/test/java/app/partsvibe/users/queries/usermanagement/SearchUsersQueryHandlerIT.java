package app.partsvibe.users.queries.usermanagement;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.shared.cqrs.PageResult;
import app.partsvibe.users.domain.Role;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;

class SearchUsersQueryHandlerIT extends AbstractUsersIntegrationTest {
    private static final String ROLE_IT_FILTER = "ROLE_IT_FILTER";
    private static final String ROLE_IT_ALPHA = "ROLE_IT_ALPHA";
    private static final String ROLE_IT_BETA = "ROLE_IT_BETA";

    @Autowired
    private SearchUsersQueryHandler queryHandler;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Override
    protected void beforeEachTest(TestInfo testInfo) {
        ensureRoleExists(ROLE_IT_FILTER);
        ensureRoleExists(ROLE_IT_ALPHA);
        ensureRoleExists(ROLE_IT_BETA);
    }

    @Test
    void usernameContainsFiltersUsersBySubstringIgnoringCaseAndWhitespace() {
        Role role = roleRepository.findByName(ROLE_IT_FILTER).orElseThrow();
        userRepository.save(
                aUser().withUsername("it-filter-alpha").enabled().withRole(role).build());
        userRepository.save(
                aUser().withUsername("it-filter-beta").disabled().withRole(role).build());
        userRepository.save(
                aUser().withUsername("other-user").enabled().withRole(role).build());

        SearchUsersQuery query = SearchUsersQuery.builder()
                .usernameContains(" FILTER-AL ")
                .currentPage(1)
                .pageSize(10)
                .sortBy(SearchUsersQuery.SORT_BY_USERNAME)
                .sortDir(SearchUsersQuery.SORT_ASC)
                .build();

        PageResult<SearchUsersQuery.UserRow> result = queryHandler.handle(query);

        assertThat(result.items())
                .extracting(SearchUsersQuery.UserRow::username)
                .containsExactly("it-filter-alpha");
    }

    @Test
    void enabledIsFiltersUsersByEnabledFlag() {
        Role role = roleRepository.findByName(ROLE_IT_FILTER).orElseThrow();
        userRepository.save(
                aUser().withUsername("it-enabled-true").enabled().withRole(role).build());
        userRepository.save(aUser().withUsername("it-enabled-false")
                .disabled()
                .withRole(role)
                .build());
        userRepository.save(aUser().withUsername("it-enabled-true-2")
                .enabled()
                .withRole(role)
                .build());

        SearchUsersQuery query = SearchUsersQuery.builder()
                .usernameContains("it-enabled")
                .enabledIs(false)
                .currentPage(1)
                .pageSize(10)
                .sortBy(SearchUsersQuery.SORT_BY_USERNAME)
                .sortDir(SearchUsersQuery.SORT_ASC)
                .build();

        PageResult<SearchUsersQuery.UserRow> result = queryHandler.handle(query);

        assertThat(result.items())
                .extracting(SearchUsersQuery.UserRow::username)
                .containsExactly("it-enabled-false");
        assertThat(result.items()).allMatch(row -> !row.enabled());
    }

    @Test
    void rolesContainAllRequiresEverySelectedRole() {
        Role alpha = roleRepository.findByName(ROLE_IT_ALPHA).orElseThrow();
        Role beta = roleRepository.findByName(ROLE_IT_BETA).orElseThrow();

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
                .rolesContainAll(List.of(ROLE_IT_ALPHA, ROLE_IT_BETA))
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

    @Test
    void appliesPagingAndPageSizeToSortedResults() {
        Role role = roleRepository.findByName(ROLE_IT_FILTER).orElseThrow();
        createUsers("it-page-user-", 12, role, true);

        SearchUsersQuery firstPageQuery = SearchUsersQuery.builder()
                .usernameContains("it-page-user-")
                .currentPage(1)
                .pageSize(10)
                .sortBy(SearchUsersQuery.SORT_BY_USERNAME)
                .sortDir(SearchUsersQuery.SORT_ASC)
                .build();
        SearchUsersQuery secondPageQuery = SearchUsersQuery.builder()
                .usernameContains("it-page-user-")
                .currentPage(2)
                .pageSize(10)
                .sortBy(SearchUsersQuery.SORT_BY_USERNAME)
                .sortDir(SearchUsersQuery.SORT_ASC)
                .build();

        PageResult<SearchUsersQuery.UserRow> firstPage = queryHandler.handle(firstPageQuery);
        PageResult<SearchUsersQuery.UserRow> secondPage = queryHandler.handle(secondPageQuery);

        assertThat(firstPage.currentPage()).isEqualTo(1);
        assertThat(firstPage.pageSize()).isEqualTo(10);
        assertThat(firstPage.totalRows()).isEqualTo(12);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(firstPage.items()).hasSize(10);
        assertThat(firstPage.items())
                .extracting(SearchUsersQuery.UserRow::username)
                .containsExactly(
                        "it-page-user-01",
                        "it-page-user-02",
                        "it-page-user-03",
                        "it-page-user-04",
                        "it-page-user-05",
                        "it-page-user-06",
                        "it-page-user-07",
                        "it-page-user-08",
                        "it-page-user-09",
                        "it-page-user-10");

        assertThat(secondPage.currentPage()).isEqualTo(2);
        assertThat(secondPage.pageSize()).isEqualTo(10);
        assertThat(secondPage.totalRows()).isEqualTo(12);
        assertThat(secondPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.items()).hasSize(2);
        assertThat(secondPage.items())
                .extracting(SearchUsersQuery.UserRow::username)
                .containsExactly("it-page-user-11", "it-page-user-12");
    }

    @Test
    void appliesConfiguredPageSizeToResultWindow() {
        Role role = roleRepository.findByName(ROLE_IT_FILTER).orElseThrow();
        createUsers("it-page-size-user-", 12, role, true);

        SearchUsersQuery query = SearchUsersQuery.builder()
                .usernameContains("it-page-size-user-")
                .currentPage(1)
                .pageSize(25)
                .sortBy(SearchUsersQuery.SORT_BY_USERNAME)
                .sortDir(SearchUsersQuery.SORT_ASC)
                .build();

        PageResult<SearchUsersQuery.UserRow> result = queryHandler.handle(query);

        assertThat(result.currentPage()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(25);
        assertThat(result.totalRows()).isEqualTo(12);
        assertThat(result.totalPages()).isEqualTo(1);
        assertThat(result.items()).hasSize(12);
    }

    @Test
    void clampsCurrentPageToLastPageWhenRequestedPageIsBeyondTotalPages() {
        Role role = roleRepository.findByName(ROLE_IT_FILTER).orElseThrow();
        createUsers("it-page-overflow-user-", 12, role, true);

        SearchUsersQuery query = SearchUsersQuery.builder()
                .usernameContains("it-page-overflow-user-")
                .currentPage(3)
                .pageSize(10)
                .sortBy(SearchUsersQuery.SORT_BY_USERNAME)
                .sortDir(SearchUsersQuery.SORT_ASC)
                .build();

        PageResult<SearchUsersQuery.UserRow> result = queryHandler.handle(query);

        assertThat(result.currentPage()).isEqualTo(2);
        assertThat(result.pageSize()).isEqualTo(10);
        assertThat(result.totalRows()).isEqualTo(12);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.items())
                .extracting(SearchUsersQuery.UserRow::username)
                .containsExactly("it-page-overflow-user-11", "it-page-overflow-user-12");
    }

    @Test
    void sortsByUsernameInDescendingOrderWhenRequested() {
        Role role = roleRepository.findByName(ROLE_IT_FILTER).orElseThrow();
        createUsers("it-sort-desc-user-", 3, role, true);

        SearchUsersQuery query = SearchUsersQuery.builder()
                .usernameContains("it-sort-desc-user-")
                .currentPage(1)
                .pageSize(10)
                .sortBy(SearchUsersQuery.SORT_BY_USERNAME)
                .sortDir(SearchUsersQuery.SORT_DESC)
                .build();

        PageResult<SearchUsersQuery.UserRow> result = queryHandler.handle(query);

        assertThat(result.items())
                .extracting(SearchUsersQuery.UserRow::username)
                .containsExactly("it-sort-desc-user-03", "it-sort-desc-user-02", "it-sort-desc-user-01");
    }

    @Test
    void returnsEmptyWhenRolesContainAllIncludesUnknownRole() {
        Role alpha = roleRepository.findByName(ROLE_IT_ALPHA).orElseThrow();
        userRepository.save(
                aUser().withUsername("it-alpha-only").enabled().withRole(alpha).build());

        SearchUsersQuery query = SearchUsersQuery.builder()
                .usernameContains("it-alpha")
                .rolesContainAll(List.of(ROLE_IT_ALPHA, "ROLE_IT_UNKNOWN"))
                .currentPage(1)
                .pageSize(10)
                .sortBy(SearchUsersQuery.SORT_BY_USERNAME)
                .sortDir(SearchUsersQuery.SORT_ASC)
                .build();

        PageResult<SearchUsersQuery.UserRow> result = queryHandler.handle(query);

        assertThat(result.totalRows()).isZero();
        assertThat(result.items()).isEmpty();
    }

    private void createUsers(String usernamePrefix, int count, Role role, boolean enabled) {
        List<app.partsvibe.users.domain.User> users = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            var userBuilder = aUser().withUsername(usernamePrefix + String.format("%02d", i))
                    .withRole(role)
                    .withPasswordHash("{noop}x");
            users.add(
                    enabled
                            ? userBuilder.enabled().build()
                            : userBuilder.disabled().build());
        }
        userRepository.saveAll(users);
    }

    private void ensureRoleExists(String roleName) {
        roleRepository
                .findByName(roleName)
                .orElseGet(() -> roleRepository.save(aRole().withName(roleName).build()));
    }
}
