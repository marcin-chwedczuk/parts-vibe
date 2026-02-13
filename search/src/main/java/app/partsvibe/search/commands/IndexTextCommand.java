package app.partsvibe.search.commands;

import app.partsvibe.shared.cqrs.Command;

public record IndexTextCommand(String text) implements Command<IndexTextCommandResult> {}
