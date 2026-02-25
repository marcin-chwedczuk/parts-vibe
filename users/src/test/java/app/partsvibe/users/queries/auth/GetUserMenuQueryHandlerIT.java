package app.partsvibe.users.queries.auth;

import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.users.errors.UserNotFoundException;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class GetUserMenuQueryHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private GetUserMenuQueryHandler queryHandler;

    @Autowired
    private UserRepository userRepository;

    @Test
    void returnsAvatarIdForExistingUser() {
        // given
        UUID avatarId = UUID.randomUUID();
        var user = aUser().withUsername("menu-user@example.com").build();
        user.setAvatarId(avatarId);
        user = userRepository.save(user);

        // when
        var result = queryHandler.handle(new GetUserMenuQuery(user.getId()));

        // then
        assertThat(result.avatarId()).isEqualTo(avatarId);
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        // given / when / then
        assertThatThrownBy(() -> queryHandler.handle(new GetUserMenuQuery(999_999L)))
                .isInstanceOf(UserNotFoundException.class);
    }
}
