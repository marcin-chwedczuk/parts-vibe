package app.partsvibe.users.events.handling;

import static org.assertj.core.api.Assertions.assertThat;

import app.partsvibe.testsupport.fakes.InMemoryMediator;
import app.partsvibe.users.email.RenderedEmail;
import app.partsvibe.users.events.UserInvitedEvent;
import app.partsvibe.users.queries.email.GetUserPreferredLocaleQuery;
import app.partsvibe.users.queries.email.RenderInviteEmailQuery;
import app.partsvibe.users.test.fakes.InMemoryEmailSender;
import app.partsvibe.users.test.it.AbstractUsersIntegrationTest;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
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

        AtomicReference<RenderInviteEmailQuery> renderQueryRef = new AtomicReference<>();
        mediator.onQuery(GetUserPreferredLocaleQuery.class, query -> Locale.ENGLISH);
        mediator.onQuery(RenderInviteEmailQuery.class, query -> {
            renderQueryRef.set(query);
            return new RenderedEmail("Invite subject", "Invite text", "<p>Invite html</p>");
        });

        var event = UserInvitedEvent.create(
                "invitee@example.com", "invite-token", Instant.parse("2026-02-26T12:00:00Z"), "Welcome", "ROLE_USER");

        // when
        handler.handle(event);

        // then
        assertThat(emailSender.sentEmails()).hasSize(1);
        var email = emailSender.sentEmails().get(0);
        assertThat(email.to()).containsExactly("invitee@example.com");
        assertThat(email.subject()).isEqualTo("Invite subject");
        assertThat(email.bodyText()).isEqualTo("Invite text");
        assertThat(email.bodyHtml()).isEqualTo("<p>Invite html</p>");
        assertThat(renderQueryRef.get()).isNotNull();
        assertThat(renderQueryRef.get().model().resetUrl()).contains("invite-token");
        assertThat(renderQueryRef.get().model().appBaseUrl()).isEqualTo("http://localhost:8080");
    }
}
