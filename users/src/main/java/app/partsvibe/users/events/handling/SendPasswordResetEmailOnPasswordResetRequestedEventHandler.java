package app.partsvibe.users.events.handling;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.shared.email.EmailMessage;
import app.partsvibe.shared.email.EmailSender;
import app.partsvibe.shared.events.handling.BaseEventHandler;
import app.partsvibe.shared.events.handling.HandlesEvent;
import app.partsvibe.users.config.UsersAuthProperties;
import app.partsvibe.users.email.templates.PasswordResetEmailModel;
import app.partsvibe.users.events.PasswordResetRequestedEvent;
import app.partsvibe.users.queries.email.GetUserPreferredLocaleQuery;
import app.partsvibe.users.queries.email.RenderPasswordResetEmailQuery;
import org.springframework.stereotype.Component;

@Component
@HandlesEvent(name = PasswordResetRequestedEvent.EVENT_NAME, version = 1)
class SendPasswordResetEmailOnPasswordResetRequestedEventHandler extends BaseEventHandler<PasswordResetRequestedEvent> {
    private final Mediator mediator;
    private final EmailSender emailSender;
    private final UsersAuthProperties usersAuthProperties;

    SendPasswordResetEmailOnPasswordResetRequestedEventHandler(
            Mediator mediator, EmailSender emailSender, UsersAuthProperties usersAuthProperties) {
        this.mediator = mediator;
        this.emailSender = emailSender;
        this.usersAuthProperties = usersAuthProperties;
    }

    @Override
    protected void doHandle(PasswordResetRequestedEvent event) {
        var locale = mediator.executeQuery(new GetUserPreferredLocaleQuery(event.email()));
        String baseUrl = usersAuthProperties.getBaseUrl();
        var rendered = mediator.executeQuery(new RenderPasswordResetEmailQuery(
                new PasswordResetEmailModel(
                        baseUrl + "/password-reset?token=" + event.token(),
                        event.expiresAt(),
                        baseUrl + "/resources/images/logo-full.png",
                        baseUrl),
                locale));

        emailSender.send(EmailMessage.builder()
                .to(event.email())
                .subject(rendered.subject())
                .bodyText(rendered.bodyText())
                .bodyHtml(rendered.bodyHtml())
                .build());
    }
}
