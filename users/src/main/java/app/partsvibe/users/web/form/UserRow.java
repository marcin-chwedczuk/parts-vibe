package app.partsvibe.users.web.form;

import java.util.List;

public record UserRow(Long id, String username, boolean enabled, List<String> roles) {
    public String rolesDisplay() {
        return String.join(", ", roles);
    }
}
