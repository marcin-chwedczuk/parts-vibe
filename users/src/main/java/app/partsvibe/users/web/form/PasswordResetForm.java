package app.partsvibe.users.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PasswordResetForm {
    @NotBlank(message = "{auth.reset.validation.token.required}")
    @Size(max = 256, message = "{auth.reset.validation.token.length}")
    private String token;

    @NotBlank(message = "{auth.reset.validation.password.required}")
    @Size(min = 12, max = 512, message = "{auth.reset.validation.password.length}")
    private String password;

    @NotBlank(message = "{auth.reset.validation.repeat.required}")
    @Size(min = 12, max = 512, message = "{auth.reset.validation.repeat.length}")
    private String repeatedPassword;
}
