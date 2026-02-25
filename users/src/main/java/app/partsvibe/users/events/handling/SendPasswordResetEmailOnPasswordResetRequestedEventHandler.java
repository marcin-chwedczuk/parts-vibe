package app.partsvibe.users.events.handling;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.shared.email.EmailMessage;
import app.partsvibe.shared.email.EmailSender;
import app.partsvibe.shared.events.handling.BaseEventHandler;
import app.partsvibe.shared.events.handling.HandlesEvent;
import app.partsvibe.users.email.ThymeleafEmailTemplateRenderer;
import app.partsvibe.users.email.templates.PasswordResetEmailModel;
import app.partsvibe.users.email.templates.PasswordResetEmailTemplate;
import app.partsvibe.users.events.PasswordResetRequestedEvent;
import app.partsvibe.users.queries.email.GetUserPreferredLocaleQuery;
import app.partsvibe.users.security.links.UserAuthLinkBuilder;
import org.springframework.stereotype.Component;

@Component
@HandlesEvent(name = PasswordResetRequestedEvent.EVENT_NAME, version = 1)
class SendPasswordResetEmailOnPasswordResetRequestedEventHandler extends BaseEventHandler<PasswordResetRequestedEvent> {
    private final Mediator mediator;
    private final EmailSender emailSender;
    private final ThymeleafEmailTemplateRenderer emailTemplateRenderer;
    private final PasswordResetEmailTemplate passwordResetEmailTemplate;
    private final UserAuthLinkBuilder authLinkBuilder;

    SendPasswordResetEmailOnPasswordResetRequestedEventHandler(
            Mediator mediator,
            EmailSender emailSender,
            ThymeleafEmailTemplateRenderer emailTemplateRenderer,
            PasswordResetEmailTemplate passwordResetEmailTemplate,
            UserAuthLinkBuilder authLinkBuilder) {
        this.mediator = mediator;
        this.emailSender = emailSender;
        this.emailTemplateRenderer = emailTemplateRenderer;
        this.passwordResetEmailTemplate = passwordResetEmailTemplate;
        this.authLinkBuilder = authLinkBuilder;
    }

    @Override
    protected void doHandle(PasswordResetRequestedEvent event) {
        var locale = mediator.executeQuery(new GetUserPreferredLocaleQuery(event.email()));
        var rendered = emailTemplateRenderer.render(
                passwordResetEmailTemplate,
                new PasswordResetEmailModel(
                        authLinkBuilder.passwordResetLink(event.token()),
                        event.expiresAt(),
                        authLinkBuilder.appLogoUrl(),
                        authLinkBuilder.appBaseUrl()),
                locale);

        emailSender.send(EmailMessage.builder()
                .to(event.email())
                .subject(rendered.subject())
                .bodyText(rendered.bodyText())
                .bodyHtml(rendered.bodyHtml())
                .build());
    }
}
