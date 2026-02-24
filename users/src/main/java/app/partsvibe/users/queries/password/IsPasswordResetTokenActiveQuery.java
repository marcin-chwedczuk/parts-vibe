package app.partsvibe.users.queries.password;

import app.partsvibe.shared.cqrs.Query;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record IsPasswordResetTokenActiveQuery(@NotBlank @Size(max = 256) String token) implements Query<Boolean> {}
