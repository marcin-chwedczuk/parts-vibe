package app.partsvibe.users.commands.provision;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.cqrs.NoResult;
import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.UserAccount;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserAccountRepository;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class SeedUsersCommandHandler extends BaseCommandHandler<SeedUsersCommand, NoResult> {
    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    SeedUsersCommandHandler(
            RoleRepository roleRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected NoResult doHandle(SeedUsersCommand command) {
        var adminRole =
                roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));
        var userRole =
                roleRepository.findByName("ROLE_USER").orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

        for (var userDefinition : command.users()) {
            userAccountRepository.findByUsername(userDefinition.username()).orElseGet(() -> {
                var user =
                        new UserAccount(userDefinition.username(), passwordEncoder.encode(userDefinition.password()));
                if (userDefinition.isAdmin()) {
                    user.getRoles().addAll(Set.of(adminRole, userRole));
                } else {
                    user.getRoles().add(userRole);
                }
                return userAccountRepository.save(user);
            });
        }

        return NoResult.INSTANCE;
    }
}
