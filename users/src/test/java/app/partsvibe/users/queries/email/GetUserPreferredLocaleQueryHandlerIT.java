package app.partsvibe.users.queries.email;

import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class GetUserPreferredLocaleQueryHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private GetUserPreferredLocaleQueryHandler queryHandler;

    @Test
    void returnsEnglishLocalePlaceholder() {
        // given / when
        Locale locale = queryHandler.handle(new GetUserPreferredLocaleQuery("user@example.com"));

        // then
        assertThat(locale).isEqualTo(Locale.ENGLISH);
    }
}
