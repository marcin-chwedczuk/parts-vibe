package app.partsvibe.users.queries.email;

import app.partsvibe.shared.cqrs.BaseQueryHandler;
import app.partsvibe.users.email.RenderedEmail;
import app.partsvibe.users.email.ThymeleafEmailTemplateRenderer;
import app.partsvibe.users.email.templates.InviteEmailTemplate;
import org.springframework.stereotype.Component;

@Component
class RenderInviteEmailQueryHandler extends BaseQueryHandler<RenderInviteEmailQuery, RenderedEmail> {
    private final ThymeleafEmailTemplateRenderer renderer;
    private final InviteEmailTemplate inviteEmailTemplate;

    RenderInviteEmailQueryHandler(ThymeleafEmailTemplateRenderer renderer, InviteEmailTemplate inviteEmailTemplate) {
        this.renderer = renderer;
        this.inviteEmailTemplate = inviteEmailTemplate;
    }

    @Override
    protected RenderedEmail doHandle(RenderInviteEmailQuery query) {
        return renderer.render(inviteEmailTemplate, query.model(), query.locale());
    }
}
