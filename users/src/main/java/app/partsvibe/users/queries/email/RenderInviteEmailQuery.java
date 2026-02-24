package app.partsvibe.users.queries.email;

import app.partsvibe.shared.cqrs.Query;
import app.partsvibe.users.email.RenderedEmail;
import app.partsvibe.users.email.templates.InviteEmailModel;
import jakarta.validation.constraints.NotNull;
import java.util.Locale;

public record RenderInviteEmailQuery(@NotNull InviteEmailModel model, @NotNull Locale locale)
        implements Query<RenderedEmail> {}
