package app.partsvibe.users.queries.email;

import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.users.email.templates.InviteEmailModel;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import java.time.Instant;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RenderInviteEmailQueryHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private RenderInviteEmailQueryHandler queryHandler;

    @Test
    void rendersInviteEmailHtmlAndText() {
        // given
        var model = new InviteEmailModel(
                "http://localhost:8080/password-reset?token=invite-token",
                Instant.parse("2026-02-26T12:00:00Z"),
                "ROLE_USER",
                "Welcome to the club",
                "http://localhost:8080/resources/images/logo-full.png",
                "http://localhost:8080");

        // when
        var rendered = queryHandler.handle(new RenderInviteEmailQuery(model, Locale.ENGLISH));

        // then
        assertThat(rendered.subject()).isNotBlank();
        assertThat(rendered.bodyHtml()).contains("invite-token");
        assertThat(rendered.bodyHtml()).contains("Welcome to the club");
        assertThat(rendered.bodyText()).contains("invite-token");
        assertThat(rendered.bodyText()).contains("ROLE_USER");
    }
}
