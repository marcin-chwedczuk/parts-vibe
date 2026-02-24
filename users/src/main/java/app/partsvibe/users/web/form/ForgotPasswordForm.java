package app.partsvibe.users.web.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordForm implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "{auth.forgot.validation.email.required}")
    @Email(message = "{auth.forgot.validation.email.invalid}")
    @Size(max = 64, message = "{auth.forgot.validation.email.length}")
    private String email;
}
