package app.partsvibe.users.queries.password;

import app.partsvibe.shared.cqrs.Query;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Optional;

public record ResolveInviteTokenContextQuery(@NotBlank @Size(max = 256) String token)
        implements Query<Optional<ResolveInviteTokenContextQuery.TokenContext>> {
    public record TokenContext(String username, InviteTokenMode mode) {}

    public enum InviteTokenMode {
        ACTIVE,
        ALREADY_REGISTERED
    }
}
