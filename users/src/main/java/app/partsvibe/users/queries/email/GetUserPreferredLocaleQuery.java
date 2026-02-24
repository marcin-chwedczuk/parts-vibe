package app.partsvibe.users.queries.email;

import app.partsvibe.shared.cqrs.Query;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record GetUserPreferredLocaleQuery(@NotBlank @Email @Size(max = 64) String email) implements Query<Locale> {}
