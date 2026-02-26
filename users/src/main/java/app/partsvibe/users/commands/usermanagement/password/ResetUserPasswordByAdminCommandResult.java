package app.partsvibe.users.commands.usermanagement.password;

public record ResetUserPasswordByAdminCommandResult(
        Long targetUserId, String targetUsername, String temporaryPassword) {
    @Override
    public String toString() {
        return "ResetUserPasswordByAdminCommandResult[targetUserId=%s, targetUsername=%s, temporaryPassword=****]"
                .formatted(targetUserId, targetUsername);
    }
}
