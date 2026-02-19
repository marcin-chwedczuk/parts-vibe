package app.partsvibe.users.commands.usermanagement;

import static java.util.Comparator.naturalOrder;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.errors.UsernameAlreadyExistsException;
import app.partsvibe.users.models.UserDetailsModel;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
class CreateUserCommandHandler extends BaseCommandHandler<CreateUserCommand, UserDetailsModel> {
    private static final String ROLE_USER = "ROLE_USER";
    private static final String PENDING_PASSWORD_HASH = "{noop}PENDING_ACTIVATION";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    CreateUserCommandHandler(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    protected UserDetailsModel doHandle(CreateUserCommand command) {
        String canonicalUsername = canonicalizeUsername(command.username());
        if (userRepository.existsByUsernameIgnoreCase(canonicalUsername)) {
            throw new UsernameAlreadyExistsException(canonicalUsername);
        }

        Role userRole = roleRepository.findByName(ROLE_USER).orElseGet(() -> roleRepository.save(new Role(ROLE_USER)));

        User user = new User(canonicalUsername, PENDING_PASSWORD_HASH);
        user.setEnabled(command.enabled());
        user.getRoles().addAll(Set.of(userRole));
        User saved = userRepository.save(user);

        return toModel(saved);
    }

    private UserDetailsModel toModel(User user) {
        return new UserDetailsModel(
                user.getId(),
                user.getUsername(),
                user.isEnabled(),
                user.getRoles().stream()
                        .map(Role::getName)
                        .sorted(naturalOrder())
                        .toList());
    }

    private String canonicalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
