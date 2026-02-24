package app.partsvibe.users.email.templates;

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
    public String textTemplateName() {
        return "email/invite-text";
    }

    @Override
    public Map<String, Object> variables(InviteEmailModel model, Locale locale) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("logoUrl", model.logoUrl());
        vars.put("appBaseUrl", model.appBaseUrl());
        vars.put("resetUrl", model.resetUrl());
        vars.put("invitedRole", model.invitedRole());
        String expiresAtFormatted = formatDateTime(model.expiresAt(), locale);
        vars.put("expiresAtFormatted", expiresAtFormatted);
        vars.put(
                "inviteMessage",
                model.inviteMessage() != null ? model.inviteMessage().trim() : null);
        vars.put("textLine1", messageSource.getMessage("email.invite.text.line1", null, locale));
        vars.put(
                "textRoleLine",
                messageSource.getMessage("email.invite.text.role", new Object[] {model.invitedRole()}, locale));
        vars.put("textLinkIntro", messageSource.getMessage("email.invite.text.link", null, locale));
        vars.put(
                "textExpiresLine",
                messageSource.getMessage("email.common.expiresAt", new Object[] {expiresAtFormatted}, locale));
        vars.put("textAdminMessageTitle", messageSource.getMessage("email.invite.text.adminMessage", null, locale));
        return vars;
    }

    private static String formatDateTime(java.time.Instant instant, Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale)
                .withZone(ZoneOffset.UTC)
                .format(instant);
    }
}
