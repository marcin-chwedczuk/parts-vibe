package app.partsvibe.users.events.handling;

import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.testsupport.fakes.InMemoryMediator;
import app.partsvibe.users.events.PasswordResetRequestedEvent;
import app.partsvibe.users.queries.email.GetUserPreferredLocaleQuery;
import app.partsvibe.users.test.fakes.InMemoryEmailSender;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import java.time.Instant;
import java.util.Locale;
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

        mediator.onQuery(GetUserPreferredLocaleQuery.class, query -> Locale.ENGLISH);

        var event = PasswordResetRequestedEvent.builder()
                .email("reset@example.com")
                .token("reset-token")
                .expiresAt(Instant.parse("2026-02-26T12:00:00Z"))
                .build();

        // when
        handler.handle(event);

        // then
        assertThat(emailSender.sentEmails()).hasSize(1);
        var email = emailSender.sentEmails().getFirst();
        assertThat(email.to()).containsExactly("reset@example.com");
        assertThat(email.subject()).isNotBlank();
        assertThat(email.bodyText()).contains("http://localhost:8080/password-reset?token=reset-token");
        assertThat(email.bodyHtml()).contains("http://localhost:8080/password-reset?token=reset-token");
        assertThat(email.bodyHtml()).contains("http://localhost:8080/resources/images/logo-full.png");
    }
}
