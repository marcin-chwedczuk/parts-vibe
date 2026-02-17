package app.partsvibe.users.web.form;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserManagementFilters {
    private String username = "";
    private String enabled = "all";
    private List<String> roles = new ArrayList<>();
    private int page = 1;
    private int size = 10;
    private String sortBy = "none";
    private String sortDir = "asc";
}
