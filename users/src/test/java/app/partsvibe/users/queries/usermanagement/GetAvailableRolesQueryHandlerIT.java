package app.partsvibe.users.queries.usermanagement;

import static app.partsvibe.users.test.databuilders.RoleTestDataBuilder.aRole;
import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.users.domain.RoleNames;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class GetAvailableRolesQueryHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private GetAvailableRolesQueryHandler queryHandler;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void returnsRolesFromDatabaseSorted() {
        // given
        roleRepository.save(aRole().withName(RoleNames.USER).build());
        roleRepository.save(aRole().withName(RoleNames.ADMIN).build());

        // when
        var result = queryHandler.handle(new GetAvailableRolesQuery());

        // then
        assertThat(result).containsExactly(RoleNames.ADMIN, RoleNames.USER);
    }
}
