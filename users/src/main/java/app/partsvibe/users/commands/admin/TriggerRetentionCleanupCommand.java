package app.partsvibe.users.commands.admin;

import app.partsvibe.shared.cqrs.Command;

public record TriggerRetentionCleanupCommand() implements Command<TriggerRetentionCleanupCommandResult> {}
