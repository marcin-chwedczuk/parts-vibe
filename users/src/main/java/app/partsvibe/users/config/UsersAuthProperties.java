package app.partsvibe.users.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("app.users.auth")
@Validated
@Data
public class UsersAuthProperties {
    @NotBlank
    private String baseUrl = "http://localhost:8080";
}
