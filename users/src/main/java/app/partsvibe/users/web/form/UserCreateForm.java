package app.partsvibe.users.web.form;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserCreateForm {
    private String username = "";
    private boolean enabled = true;
}
