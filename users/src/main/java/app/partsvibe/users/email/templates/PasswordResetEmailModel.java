package app.partsvibe.users.email.templates;

import java.time.Instant;

public record PasswordResetEmailModel(String resetUrl, Instant expiresAt, String logoUrl, String appBaseUrl) {}
