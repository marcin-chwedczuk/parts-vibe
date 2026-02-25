package app.partsvibe.users.queries.email;

import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.users.email.templates.PasswordResetEmailModel;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import java.time.Instant;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RenderPasswordResetEmailQueryHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private RenderPasswordResetEmailQueryHandler queryHandler;

    @Test
    void rendersPasswordResetEmailHtmlAndText() {
        // given
        var model = new PasswordResetEmailModel(
                "http://localhost:8080/password-reset?token=reset-token",
                Instant.parse("2026-02-26T12:00:00Z"),
                "http://localhost:8080/resources/images/logo-full.png",
                "http://localhost:8080");

        // when
        var rendered = queryHandler.handle(new RenderPasswordResetEmailQuery(model, Locale.ENGLISH));

        // then
        assertThat(rendered.subject()).isNotBlank();
        assertThat(rendered.bodyHtml()).contains("reset-token");
        assertThat(rendered.bodyText()).contains("reset-token");
        assertThat(rendered.bodyHtml()).contains("logo-full.png");
    }
}
