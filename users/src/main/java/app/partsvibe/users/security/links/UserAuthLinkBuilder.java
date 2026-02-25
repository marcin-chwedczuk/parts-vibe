package app.partsvibe.users.security.links;

import app.partsvibe.users.config.UsersAuthProperties;
import org.springframework.stereotype.Component;

@Component
public class UserAuthLinkBuilder {
    private final UsersAuthProperties usersAuthProperties;

    public UserAuthLinkBuilder(UsersAuthProperties usersAuthProperties) {
        this.usersAuthProperties = usersAuthProperties;
    }

    public String inviteLink(String token) {
        return baseUrl() + "/invite?token=" + token;
    }

    public String passwordResetLink(String token) {
        return baseUrl() + "/password-reset?token=" + token;
    }

    public String appLogoUrl() {
        return baseUrl() + "/resources/images/logo-full.png";
    }

    public String appBaseUrl() {
        return baseUrl();
    }

    private String baseUrl() {
        String baseUrl = usersAuthProperties.getBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
