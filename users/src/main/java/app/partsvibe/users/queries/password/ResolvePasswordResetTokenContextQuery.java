package app.partsvibe.users.queries.password;

import app.partsvibe.shared.cqrs.Query;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Optional;

public record ResolvePasswordResetTokenContextQuery(@NotBlank @Size(max = 256) String token)
        implements Query<Optional<ResolvePasswordResetTokenContextQuery.TokenContext>> {
    public record TokenContext(String username) {}
}
