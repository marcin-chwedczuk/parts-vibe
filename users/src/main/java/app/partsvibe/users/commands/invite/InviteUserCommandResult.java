package app.partsvibe.users.commands.invite;

import java.time.Instant;

public record InviteUserCommandResult(Long userId, String email, Instant expiresAt, InviteOutcome outcome) {
    public enum InviteOutcome {
        INVITE_SENT,
        INVITE_RESENT,
        ALREADY_ONBOARDED,
        ALREADY_ONBOARDED_LOCKED
    }
}
