package app.partsvibe.users.events.handling;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.shared.email.EmailMessage;
import app.partsvibe.shared.email.EmailSender;
import app.partsvibe.shared.events.handling.BaseEventHandler;
import app.partsvibe.shared.events.handling.HandlesEvent;
import app.partsvibe.users.email.templates.InviteEmailModel;
import app.partsvibe.users.events.UserInvitedEvent;
import app.partsvibe.users.queries.email.GetUserPreferredLocaleQuery;
import app.partsvibe.users.queries.email.RenderInviteEmailQuery;
import app.partsvibe.users.security.links.UserAuthLinkBuilder;
import org.springframework.stereotype.Component;

@Component
@HandlesEvent(name = UserInvitedEvent.EVENT_NAME, version = 1)
class SendInviteEmailOnUserInvitedEventHandler extends BaseEventHandler<UserInvitedEvent> {
    private final Mediator mediator;
    private final EmailSender emailSender;
    private final UserAuthLinkBuilder authLinkBuilder;

    SendInviteEmailOnUserInvitedEventHandler(
            Mediator mediator, EmailSender emailSender, UserAuthLinkBuilder authLinkBuilder) {
        this.mediator = mediator;
        this.emailSender = emailSender;
        this.authLinkBuilder = authLinkBuilder;
    }

    @Override
    protected void doHandle(UserInvitedEvent event) {
        var locale = mediator.executeQuery(new GetUserPreferredLocaleQuery(event.email()));
        var rendered = mediator.executeQuery(new RenderInviteEmailQuery(
                new InviteEmailModel(
                        authLinkBuilder.inviteLink(event.token()),
                        event.expiresAt(),
                        event.invitedRole(),
                        event.inviteMessage(),
                        authLinkBuilder.appLogoUrl(),
                        authLinkBuilder.appBaseUrl()),
                locale));

        emailSender.send(EmailMessage.builder()
                .to(event.email())
                .subject(rendered.subject())
                .bodyText(rendered.bodyText())
                .bodyHtml(rendered.bodyHtml())
                .build());
    }
}
