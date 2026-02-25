package app.partsvibe.users.web.form;

import app.partsvibe.users.domain.RoleNames;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serial;
import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InviteUserForm implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @NotBlank(message = "{admin.user.invite.validation.email.required}")
    @Email(message = "{admin.user.invite.validation.email.invalid}")
    @Size(max = 64, message = "{admin.user.invite.validation.email.length}")
    private String email;

    @NotBlank(message = "{admin.user.invite.validation.role.required}")
    private String roleName = RoleNames.USER;

    @NotBlank(message = "{admin.user.invite.validation.validity.required}")
    private String validity = "24h";

    @Size(max = 1000, message = "{admin.user.invite.validation.message.length}")
    private String inviteMessage;
}
