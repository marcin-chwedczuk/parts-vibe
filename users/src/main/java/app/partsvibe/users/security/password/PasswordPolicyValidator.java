package app.partsvibe.users.security.password;

import app.partsvibe.users.errors.WeakPasswordException;
import java.util.Locale;

public final class PasswordPolicyValidator {
    private PasswordPolicyValidator() {}

    public static void validate(String password, String usernameOrEmail) {
        if (password.length() < 12) {
            throw new WeakPasswordException("Password must have at least 12 characters.");
        }

        String normalizedPassword = password.toLowerCase(Locale.ROOT);
        String normalizedUsername = usernameOrEmail.toLowerCase(Locale.ROOT);
        if (normalizedPassword.contains(normalizedUsername)) {
            throw new WeakPasswordException("Password cannot contain your username/email.");
        }
        int at = normalizedUsername.indexOf('@');
        if (at > 0 && normalizedPassword.contains(normalizedUsername.substring(0, at))) {
            throw new WeakPasswordException("Password cannot contain your username/email.");
        }
    }
}
