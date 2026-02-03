package app.partsvibe.users.config;

import app.partsvibe.users.service.UserProvisioningService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements ApplicationRunner {
    private final UserProvisioningService userProvisioningService;

    @Value("${app.security.admin-username}")
    private String adminUsername;

    @Value("${app.security.admin-password}")
    private String adminPassword;

    @Value("${app.security.user-username}")
    private String userUsername;

    @Value("${app.security.user-password}")
    private String userPassword;

    public DataInitializer(UserProvisioningService userProvisioningService) {
        this.userProvisioningService = userProvisioningService;
    }

    @Override
    public void run(ApplicationArguments args) {
        userProvisioningService.seedUsers(adminUsername, adminPassword, userUsername, userPassword);
    }
}
