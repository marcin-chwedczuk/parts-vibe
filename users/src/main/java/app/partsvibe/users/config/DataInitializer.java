package app.partsvibe.users.config;

import app.partsvibe.shared.cqrs.Mediator;
import app.partsvibe.users.commands.provision.SeedUsersCommand;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {
    private final Mediator mediator;

    @Value("${app.security.admin-username}")
    private String adminUsername;

    @Value("${app.security.admin-password}")
    private String adminPassword;

    @Value("${app.security.user-username}")
    private String userUsername;

    @Value("${app.security.user-password}")
    private String userPassword;

    public DataInitializer(Mediator mediator) {
        this.mediator = mediator;
    }

    @Override
    public void run(ApplicationArguments args) {
        mediator.executeCommand(new SeedUsersCommand(List.of(
                new SeedUsersCommand.UserDefinition(adminUsername, adminPassword, true),
                new SeedUsersCommand.UserDefinition(userUsername, userPassword, false),
                new SeedUsersCommand.UserDefinition("bob", "bob123", false),
                new SeedUsersCommand.UserDefinition("bob2", "bob123", false),
                new SeedUsersCommand.UserDefinition("bob3", "bob123", false),
                new SeedUsersCommand.UserDefinition("bob4", "bob123", false),
                new SeedUsersCommand.UserDefinition("bob5", "bob123", false),
                new SeedUsersCommand.UserDefinition("bob6", "bob123", false),
                new SeedUsersCommand.UserDefinition("bob7", "bob123", false),
                new SeedUsersCommand.UserDefinition("bob8", "bob123", false),
                new SeedUsersCommand.UserDefinition("bob9", "bob123", false),
                new SeedUsersCommand.UserDefinition("bob10", "bob123", false),
                new SeedUsersCommand.UserDefinition("bob11", "bob123", false),
                new SeedUsersCommand.UserDefinition("bob12", "bob123", false),
                new SeedUsersCommand.UserDefinition("alice", "alice123", false))));
    }
}
