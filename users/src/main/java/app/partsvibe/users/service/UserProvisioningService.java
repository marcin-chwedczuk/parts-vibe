package app.partsvibe.users.service;

public interface UserProvisioningService {
    void seedUsers(String adminUsername, String adminPassword, String userUsername, String userPassword);
}
