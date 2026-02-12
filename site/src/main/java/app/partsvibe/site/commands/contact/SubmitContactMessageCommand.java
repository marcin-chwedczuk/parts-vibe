package app.partsvibe.site.commands.contact;

import app.partsvibe.shared.cqrs.Command;

public record SubmitContactMessageCommand(String name, String email, String message)
        implements Command<SubmitContactMessageCommandResult> {}
