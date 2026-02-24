package app.partsvibe.users.events.handling;

import app.partsvibe.shared.email.EmailMessage;
import app.partsvibe.shared.email.EmailSender;
import app.partsvibe.shared.events.handling.BaseEventHandler;
import app.partsvibe.shared.events.handling.HandlesEvent;
import app.partsvibe.users.config.UsersAuthProperties;
import app.partsvibe.users.events.UserInvitedEvent;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
@HandlesEvent(name = UserInvitedEvent.EVENT_NAME, version = 1)
class SendInviteEmailOnUserInvitedEventHandler extends BaseEventHandler<UserInvitedEvent> {
    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final EmailSender emailSender;
    private final UsersAuthProperties usersAuthProperties;

    SendInviteEmailOnUserInvitedEventHandler(EmailSender emailSender, UsersAuthProperties usersAuthProperties) {
        this.emailSender = emailSender;
        this.usersAuthProperties = usersAuthProperties;
    }

    @Override
    protected void doHandle(UserInvitedEvent event) {
        String resetUrl = usersAuthProperties.getBaseUrl() + "/password-reset?token=" + event.token();
        StringBuilder body = new StringBuilder();
        body.append("You have been invited to Parts Vibe.\n\n")
                .append("Role: ")
                .append(event.invitedRole())
                .append("\n")
                .append("Use this link to set your password:\n")
                .append(resetUrl)
                .append("\n\n")
                .append("Link expires at: ")
                .append(TS_FORMAT.format(event.expiresAt().atOffset(ZoneOffset.UTC)))
                .append("\n");

        if (event.inviteMessage() != null && !event.inviteMessage().isBlank()) {
            body.append("\nMessage from admin:\n")
                    .append(event.inviteMessage().trim())
                    .append("\n");
        }

        emailSender.send(EmailMessage.builder()
                .to(event.email())
                .subject("You're invited to Parts Vibe")
                .bodyText(body.toString())
                .build());
    }
}
