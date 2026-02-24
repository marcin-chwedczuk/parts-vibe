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
public class PasswordResetEmailTemplate implements TypedEmailTemplate<PasswordResetEmailModel> {
    private final MessageSource messageSource;

    public PasswordResetEmailTemplate(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @Override
    public String subject(PasswordResetEmailModel model, Locale locale) {
        return messageSource.getMessage("email.passwordReset.subject", null, locale);
    }

    @Override
    public String htmlTemplateName() {
        return "email/password-reset";
    }

    @Override
    public Map<String, Object> variables(PasswordResetEmailModel model, Locale locale) {
        Map<String, Object> vars = new LinkedHashMap<>();
        vars.put("logoUrl", model.logoUrl());
        vars.put("appBaseUrl", model.appBaseUrl());
        vars.put("resetUrl", model.resetUrl());
        vars.put("expiresAtFormatted", formatDateTime(model.expiresAt(), locale));
        return vars;
    }

    @Override
    public String textBody(PasswordResetEmailModel model, Locale locale) {
        return messageSource.getMessage("email.passwordReset.text.line1", null, locale)
                + "\n\n"
                + messageSource.getMessage("email.passwordReset.text.link", null, locale)
                + "\n"
                + model.resetUrl()
                + "\n\n"
                + messageSource.getMessage(
                        "email.common.expiresAt", new Object[] {formatDateTime(model.expiresAt(), locale)}, locale)
                + "\n";
    }

    private static String formatDateTime(java.time.Instant instant, Locale locale) {
        return DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
                .withLocale(locale)
                .withZone(ZoneOffset.UTC)
                .format(instant);
    }
}
