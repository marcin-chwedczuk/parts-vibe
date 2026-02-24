package app.partsvibe.users.email;

import java.util.Locale;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class ThymeleafEmailTemplateRenderer {
    private final TemplateEngine templateEngine;

    public ThymeleafEmailTemplateRenderer(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public <T> RenderedEmail render(TypedEmailTemplate<T> template, T model, Locale locale) {
        Context context = new Context(locale);
        context.setVariables(template.variables(model, locale));

        String htmlBody = templateEngine.process(template.htmlTemplateName(), context);
        String textBody = template.textBody(model, locale);
        String subject = template.subject(model, locale);

        return new RenderedEmail(subject, textBody, htmlBody);
    }
}
