package app.partsvibe.users.events.handling;

import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.testsupport.fakes.InMemoryMediator;
import app.partsvibe.users.events.UserInvitedEvent;
import app.partsvibe.users.queries.email.GetUserPreferredLocaleQuery;
import app.partsvibe.users.test.fakes.InMemoryEmailSender;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import java.time.Instant;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SendInviteEmailOnUserInvitedEventHandlerIT extends AbstractUsersIntegrationTest {
    @Autowired
    private SendInviteEmailOnUserInvitedEventHandler handler;

    @Autowired
    private InMemoryMediator mediator;

    @Autowired
    private InMemoryEmailSender emailSender;

    @Test
    void rendersAndSendsInviteEmail() {
        // given
        mediator.clear();
        emailSender.clear();

        mediator.onQuery(GetUserPreferredLocaleQuery.class, query -> Locale.ENGLISH);

        var event = UserInvitedEvent.create(
                "invitee@example.com", "invite-token", Instant.parse("2026-02-26T12:00:00Z"), "Welcome", "ROLE_USER");

        // when
        handler.handle(event);

        // then
        assertThat(emailSender.sentEmails()).hasSize(1);
        var email = emailSender.sentEmails().get(0);
        assertThat(email.to()).containsExactly("invitee@example.com");
        assertThat(email.subject()).isNotBlank();
        assertThat(email.bodyText()).contains("http://localhost:8080/invite?token=invite-token");
        assertThat(email.bodyHtml()).contains("http://localhost:8080/invite?token=invite-token");
        assertThat(email.bodyHtml()).contains("http://localhost:8080/resources/images/logo-full.png");
    }
}
