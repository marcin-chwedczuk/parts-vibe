package app.partsvibe.users.queries.profile;

import static app.partsvibe.users.test.databuilders.UserTestDataBuilder.aUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import app.partsvibe.users.errors.UserNotFoundException;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class GetCurrentUserProfileQueryHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private GetCurrentUserProfileQueryHandler queryHandler;

    @Autowired
    private UserRepository userRepository;

    @Test
    void returnsProfileDataForExistingUser() {
        // given
        UUID avatarId = UUID.randomUUID();
        var user = userRepository.save(aUser().withUsername("profile-user@example.com")
                .withBio("A short bio")
                .withWebsite("https://example.com")
                .withAvatarId(avatarId)
                .build());

        // when
        var result = queryHandler.handle(new GetCurrentUserProfileQuery(user.getId()));

        // then
        assertThat(result.id()).isEqualTo(user.getId());
        assertThat(result.username()).isEqualTo("profile-user@example.com");
        assertThat(result.bio()).isEqualTo("A short bio");
        assertThat(result.website()).isEqualTo("https://example.com");
        assertThat(result.avatarId()).isEqualTo(avatarId);
    }

    @Test
    void throwsWhenUserDoesNotExist() {
        // given / when / then
        assertThatThrownBy(() -> queryHandler.handle(new GetCurrentUserProfileQuery(999_999L)))
                .isInstanceOf(UserNotFoundException.class);
    }
}
