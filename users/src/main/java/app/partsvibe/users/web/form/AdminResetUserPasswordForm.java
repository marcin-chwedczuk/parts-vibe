package app.partsvibe.users.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminResetUserPasswordForm {
    @NotBlank(message = "{admin.user.passwordReset.validation.adminPassword.required}")
    @Size(max = 512, message = "{admin.user.passwordReset.validation.adminPassword.length}")
    private String adminPassword;
}
