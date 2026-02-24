package app.partsvibe.users.email;

import java.util.Locale;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
public class ThymeleafEmailTemplateRenderer {
    private final TemplateEngine htmlTemplateEngine;
    private final TemplateEngine textTemplateEngine;

    public ThymeleafEmailTemplateRenderer(
            @Qualifier("templateEngine") TemplateEngine htmlTemplateEngine,
            @Qualifier("emailTextTemplateEngine") TemplateEngine textTemplateEngine) {
        this.htmlTemplateEngine = htmlTemplateEngine;
        this.textTemplateEngine = textTemplateEngine;
    }

    public <T> RenderedEmail render(TypedEmailTemplate<T> template, T model, Locale locale) {
        Context context = new Context(locale);
        context.setVariables(template.variables(model, locale));

        String htmlBody = htmlTemplateEngine.process(template.htmlTemplateName(), context);
        String textBody = textTemplateEngine.process(template.textTemplateName(), context);
        String subject = template.subject(model, locale);

        return new RenderedEmail(subject, textBody, htmlBody);
    }
}
