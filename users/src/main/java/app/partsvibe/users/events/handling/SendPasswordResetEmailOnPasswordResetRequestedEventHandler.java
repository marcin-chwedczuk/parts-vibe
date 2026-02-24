package app.partsvibe.users.events.handling;

import app.partsvibe.shared.email.EmailMessage;
import app.partsvibe.shared.email.EmailSender;
import app.partsvibe.shared.events.handling.BaseEventHandler;
import app.partsvibe.shared.events.handling.HandlesEvent;
import app.partsvibe.users.config.UsersAuthProperties;
import app.partsvibe.users.events.PasswordResetRequestedEvent;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
@HandlesEvent(name = PasswordResetRequestedEvent.EVENT_NAME, version = 1)
class SendPasswordResetEmailOnPasswordResetRequestedEventHandler extends BaseEventHandler<PasswordResetRequestedEvent> {
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final EmailSender emailSender;
    private final UsersAuthProperties usersAuthProperties;

    SendPasswordResetEmailOnPasswordResetRequestedEventHandler(
            EmailSender emailSender, UsersAuthProperties usersAuthProperties) {
        this.emailSender = emailSender;
        this.usersAuthProperties = usersAuthProperties;
    }

    @Override
    protected void doHandle(PasswordResetRequestedEvent event) {
        String resetUrl = usersAuthProperties.getBaseUrl() + "/password-reset?token=" + event.token();
        String body = "We received a password reset request for your Parts Vibe account.\n\n"
                + "Use this link to set a new password:\n"
                + resetUrl
                + "\n\n"
                + "Link expires at: "
                + TS_FORMAT.format(event.expiresAt().atOffset(ZoneOffset.UTC))
                + "\n";

        emailSender.send(EmailMessage.builder()
                .to(event.email())
                .subject("Parts Vibe password reset")
                .bodyText(body)
                .build());
    }
}
