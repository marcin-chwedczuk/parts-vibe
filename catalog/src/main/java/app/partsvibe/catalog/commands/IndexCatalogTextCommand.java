package app.partsvibe.catalog.commands;

import app.partsvibe.shared.cqrs.Command;

public record IndexCatalogTextCommand(String text) implements Command<IndexCatalogTextCommandResult> {}
