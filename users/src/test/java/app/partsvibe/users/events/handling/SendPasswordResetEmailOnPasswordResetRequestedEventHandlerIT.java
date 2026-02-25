package app.partsvibe.users.events.handling;

import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.testsupport.fakes.InMemoryMediator;
import app.partsvibe.users.email.RenderedEmail;
import app.partsvibe.users.events.PasswordResetRequestedEvent;
import app.partsvibe.users.queries.email.GetUserPreferredLocaleQuery;
import app.partsvibe.users.queries.email.RenderPasswordResetEmailQuery;
import app.partsvibe.users.test.fakes.InMemoryEmailSender;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SendPasswordResetEmailOnPasswordResetRequestedEventHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private SendPasswordResetEmailOnPasswordResetRequestedEventHandler handler;

    @Autowired
    private InMemoryMediator mediator;

    @Autowired
    private InMemoryEmailSender emailSender;

    @Test
    void rendersAndSendsPasswordResetEmail() {
        // given
        mediator.clear();
        emailSender.clear();

        AtomicReference<RenderPasswordResetEmailQuery> renderQueryRef = new AtomicReference<>();
        mediator.onQuery(GetUserPreferredLocaleQuery.class, query -> Locale.ENGLISH);
        mediator.onQuery(RenderPasswordResetEmailQuery.class, query -> {
            renderQueryRef.set(query);
            return new RenderedEmail("Reset subject", "Reset text", "<p>Reset html</p>");
        });

        var event = PasswordResetRequestedEvent.create(
                "reset@example.com", "reset-token", Instant.parse("2026-02-26T12:00:00Z"));

        // when
        handler.handle(event);

        // then
        assertThat(emailSender.sentEmails()).hasSize(1);
        var email = emailSender.sentEmails().get(0);
        assertThat(email.to()).containsExactly("reset@example.com");
        assertThat(email.subject()).isEqualTo("Reset subject");
        assertThat(email.bodyText()).isEqualTo("Reset text");
        assertThat(email.bodyHtml()).isEqualTo("<p>Reset html</p>");
        assertThat(renderQueryRef.get()).isNotNull();
        assertThat(renderQueryRef.get().model().resetUrl())
                .isEqualTo("http://localhost:8080/password-reset?token=reset-token");
        assertThat(renderQueryRef.get().model().appBaseUrl()).isEqualTo("http://localhost:8080");
        assertThat(renderQueryRef.get().model().logoUrl())
                .isEqualTo("http://localhost:8080/resources/images/logo-full.png");
    }
}
