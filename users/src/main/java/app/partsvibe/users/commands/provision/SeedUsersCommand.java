package app.partsvibe.users.commands.provision;

import app.partsvibe.shared.cqrs.Command;
import app.partsvibe.shared.cqrs.NoResult;
import java.util.List;

public record SeedUsersCommand(List<UserDefinition> users) implements Command<NoResult> {
    public record UserDefinition(String username, String password, boolean isAdmin) {}
}
