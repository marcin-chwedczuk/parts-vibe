package app.partsvibe.users.web.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteUserForm {
    @NotBlank(message = "{admin.user.invite.validation.email.required}")
    @Email(message = "{admin.user.invite.validation.email.invalid}")
    @Size(max = 64, message = "{admin.user.invite.validation.email.length}")
    private String email;

    @NotBlank(message = "{admin.user.invite.validation.role.required}")
    private String roleName = "ROLE_USER";

    @NotBlank(message = "{admin.user.invite.validation.validity.required}")
    private String validity = "24h";

    @Size(max = 1000, message = "{admin.user.invite.validation.message.length}")
    private String inviteMessage;
}
