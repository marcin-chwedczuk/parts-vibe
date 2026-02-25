package app.partsvibe.users.queries.password;

import app.partsvibe.shared.cqrs.Query;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Optional;

public record ResolvePasswordSetupTokenQuery(@NotBlank @Size(max = 256) String token)
        implements Query<Optional<ResolvePasswordSetupTokenQuery.TokenContext>> {
    public record TokenContext(String username, SetupMode mode) {}

    public enum SetupMode {
        PASSWORD_RESET,
        INVITE,
        INVITE_ALREADY_REGISTERED
    }
}
