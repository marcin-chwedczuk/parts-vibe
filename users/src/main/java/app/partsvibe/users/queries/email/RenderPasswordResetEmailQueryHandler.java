package app.partsvibe.users.queries.email;

import app.partsvibe.shared.cqrs.BaseQueryHandler;
import app.partsvibe.users.email.RenderedEmail;
import app.partsvibe.users.email.ThymeleafEmailTemplateRenderer;
import app.partsvibe.users.email.templates.PasswordResetEmailTemplate;
import org.springframework.stereotype.Component;

@Component
class RenderPasswordResetEmailQueryHandler extends BaseQueryHandler<RenderPasswordResetEmailQuery, RenderedEmail> {
    private final ThymeleafEmailTemplateRenderer renderer;
    private final PasswordResetEmailTemplate passwordResetEmailTemplate;

    RenderPasswordResetEmailQueryHandler(
            ThymeleafEmailTemplateRenderer renderer, PasswordResetEmailTemplate passwordResetEmailTemplate) {
        this.renderer = renderer;
        this.passwordResetEmailTemplate = passwordResetEmailTemplate;
    }

    @Override
    protected RenderedEmail doHandle(RenderPasswordResetEmailQuery query) {
        return renderer.render(passwordResetEmailTemplate, query.model(), query.locale());
    }
}
