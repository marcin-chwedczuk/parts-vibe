package app.partsvibe.site.commands.testevent;

import app.partsvibe.shared.cqrs.Command;

public record PublishTestEventCommand(String requestId, String message)
        implements Command<PublishTestEventCommandResult> {}
