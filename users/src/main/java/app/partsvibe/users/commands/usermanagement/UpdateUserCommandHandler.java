package app.partsvibe.users.commands.usermanagement;

import static java.util.Comparator.naturalOrder;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.errors.UserNotFoundException;
import app.partsvibe.users.errors.UsernameAlreadyExistsException;
import app.partsvibe.users.models.UserDetailsModel;
import app.partsvibe.users.repo.UserRepository;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
class UpdateUserCommandHandler extends BaseCommandHandler<UpdateUserCommand, UserDetailsModel> {
    private final UserRepository userRepository;

    UpdateUserCommandHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected UserDetailsModel doHandle(UpdateUserCommand command) {
        User user = userRepository
                .findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        String canonicalUsername = canonicalizeUsername(command.username());
        if (userRepository.existsByUsernameIgnoreCaseAndIdNot(canonicalUsername, user.getId())) {
            throw new UsernameAlreadyExistsException(canonicalUsername);
        }

        user.setUsername(canonicalUsername);
        user.setEnabled(command.enabled());
        User saved = userRepository.save(user);

        return new UserDetailsModel(
                saved.getId(),
                saved.getUsername(),
                saved.isEnabled(),
                saved.getRoles().stream()
                        .map(Role::getName)
                        .sorted(naturalOrder())
                        .toList());
    }

    private String canonicalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
