package app.partsvibe.users.web.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserForm {
    @NotBlank(message = "{admin.user.validation.username.required}")
    @Email(message = "{admin.user.validation.username.invalidEmail}")
    @Size(max = 64, message = "{admin.user.validation.username.max}")
    private String username = "";

    private boolean enabled = true;
}
