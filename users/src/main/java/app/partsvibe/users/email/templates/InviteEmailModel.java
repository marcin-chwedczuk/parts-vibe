package app.partsvibe.users.email.templates;

import java.time.Instant;

public record InviteEmailModel(
        String resetUrl,
        Instant expiresAt,
        String invitedRole,
        String inviteMessage,
        String logoUrl,
        String appBaseUrl) {}
