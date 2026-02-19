package app.partsvibe.users.commands.usermanagement;

import app.partsvibe.shared.cqrs.Command;

public record DeleteUserCommand(Long userId) implements Command<DeleteUserCommandResult> {}
