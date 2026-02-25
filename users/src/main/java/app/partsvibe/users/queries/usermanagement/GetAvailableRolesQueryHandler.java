package app.partsvibe.users.queries.usermanagement;

import app.partsvibe.shared.cqrs.BaseQueryHandler;
import app.partsvibe.users.repo.RoleRepository;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class GetAvailableRolesQueryHandler extends BaseQueryHandler<GetAvailableRolesQuery, List<String>> {
    private final RoleRepository roleRepository;

    GetAvailableRolesQueryHandler(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    protected List<String> doHandle(GetAvailableRolesQuery query) {
        return roleRepository.findAll().stream()
                .map(role -> role.getName())
                .distinct()
                .sorted()
                .toList();
    }
}
