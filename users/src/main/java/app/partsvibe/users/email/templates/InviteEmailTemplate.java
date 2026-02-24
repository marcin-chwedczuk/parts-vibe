package app.partsvibe.users.email.templates;

import app.partsvibe.shared.utils.StringUtils;
import app.partsvibe.users.email.TypedEmailTemplate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
public class InviteEmailTemplate implements TypedEmailTemplate<InviteEmailModel> {
    private final MessageSource messageSource;

    public InviteEmailTemplate(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String subject(InviteEmailModel model, Locale locale) {
        return messageSource.getMessage("email.invite.subject", null, locale);
    }

    @Override
    public String htmlTemplateName() {
        return "email/invite";
    }

    @Override
    public Map<String, Object> variables(InviteEmailModel model, Locale locale) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("logoUrl", model.logoUrl());
        vars.put("appBaseUrl", model.appBaseUrl());
        vars.put("resetUrl", model.resetUrl());
        vars.put("invitedRole", model.invitedRole());
        vars.put("expiresAtFormatted", formatDateTime(model.expiresAt(), locale));
        vars.put(
                "inviteMessage",
                StringUtils.hasText(model.inviteMessage())
                        ? model.inviteMessage().trim()
                        : null);
        return vars;
    }

    @Override
    public String textBody(InviteEmailModel model, Locale locale) {
        StringBuilder sb = new StringBuilder();
        sb.append(messageSource.getMessage("email.invite.text.line1", null, locale))
                .append("\n\n")
                .append(messageSource.getMessage("email.invite.text.role", new Object[] {model.invitedRole()}, locale))
                .append("\n")
                .append(messageSource.getMessage("email.invite.text.link", null, locale))
                .append("\n")
                .append(model.resetUrl())
                .append("\n\n")
                .append(messageSource.getMessage(
                        "email.common.expiresAt", new Object[] {formatDateTime(model.expiresAt(), locale)}, locale))
                .append("\n");

        if (StringUtils.hasText(model.inviteMessage())) {
            sb.append("\n")
                    .append(messageSource.getMessage("email.invite.text.adminMessage", null, locale))
                    .append("\n")
                    .append(model.inviteMessage().trim())
                    .append("\n");
        }

        return sb.toString();
    }

    private static String formatDateTime(java.time.Instant instant, Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale)
                .withZone(ZoneOffset.UTC)
                .format(instant);
    }
}
