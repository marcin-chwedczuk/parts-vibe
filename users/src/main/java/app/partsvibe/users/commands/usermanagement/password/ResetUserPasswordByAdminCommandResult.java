package app.partsvibe.users.commands.usermanagement.password;

public record ResetUserPasswordByAdminCommandResult(
        Long targetUserId, String targetUsername, String temporaryPassword) {}
