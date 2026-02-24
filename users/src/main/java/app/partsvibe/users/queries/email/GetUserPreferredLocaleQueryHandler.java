package app.partsvibe.users.queries.email;

import app.partsvibe.shared.cqrs.BaseQueryHandler;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
class GetUserPreferredLocaleQueryHandler extends BaseQueryHandler<GetUserPreferredLocaleQuery, Locale> {
    @Override
    protected Locale doHandle(GetUserPreferredLocaleQuery query) {
        // TODO: Resolve locale from user profile/settings and add short-lived caching (e.g. 5-15 min).
        return Locale.ENGLISH;
    }
}
