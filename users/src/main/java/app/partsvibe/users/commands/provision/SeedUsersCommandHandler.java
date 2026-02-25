package app.partsvibe.users.commands.provision;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.cqrs.NoResult;
import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.RoleNames;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class SeedUsersCommandHandler extends BaseCommandHandler<SeedUsersCommand, NoResult> {
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    SeedUsersCommandHandler(
            RoleRepository roleRepository, UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected NoResult doHandle(SeedUsersCommand command) {
        var adminRole = roleRepository
                .findByName(RoleNames.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleNames.ADMIN)));
        var userRole = roleRepository
                .findByName(RoleNames.USER)
                .orElseGet(() -> roleRepository.save(new Role(RoleNames.USER)));

        for (var userDefinition : command.users()) {
            userRepository.findByUsernameIgnoreCase(userDefinition.username()).orElseGet(() -> {
                var user = new User(userDefinition.username(), passwordEncoder.encode(userDefinition.password()));
                if (userDefinition.isAdmin()) {
                    user.getRoles().addAll(Set.of(adminRole, userRole));
                } else {
                    user.getRoles().add(userRole);
                }
                return userRepository.save(user);
            });
        }

        return NoResult.INSTANCE;
    }
}
