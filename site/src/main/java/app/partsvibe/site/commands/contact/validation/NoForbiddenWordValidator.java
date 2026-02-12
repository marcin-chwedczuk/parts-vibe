package app.partsvibe.site.commands.contact.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Locale;

public class NoForbiddenWordValidator implements ConstraintValidator<NoForbiddenWord, String> {
    private String forbiddenWordLowercase;

    @Override
    public void initialize(NoForbiddenWord annotation) {
        forbiddenWordLowercase = annotation.value().toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return !value.toLowerCase(Locale.ROOT).contains(forbiddenWordLowercase);
    }
}
