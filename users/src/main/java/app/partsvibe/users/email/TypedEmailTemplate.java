package app.partsvibe.users.email;

import java.util.Locale;
import java.util.Map;

public interface TypedEmailTemplate<T> {
    String subject(T model, Locale locale);

    String htmlTemplateName();

    String textTemplateName();

    Map<String, Object> variables(T model, Locale locale);
}
