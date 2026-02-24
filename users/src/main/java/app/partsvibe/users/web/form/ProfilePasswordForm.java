package app.partsvibe.users.web.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProfilePasswordForm {
    @NotBlank(message = "{profile.password.validation.current.required}")
    @Size(max = 512, message = "{profile.password.validation.current.length}")
    private String currentPassword;

    @NotBlank(message = "{profile.password.validation.new.required}")
    @Size(min = 12, max = 512, message = "{profile.password.validation.new.length}")
    private String newPassword;

    @NotBlank(message = "{profile.password.validation.repeat.required}")
    @Size(min = 12, max = 512, message = "{profile.password.validation.repeat.length}")
    private String repeatedNewPassword;
}
