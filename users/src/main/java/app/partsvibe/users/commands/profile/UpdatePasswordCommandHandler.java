package app.partsvibe.users.commands.profile;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.cqrs.NoResult;
import app.partsvibe.shared.security.CurrentUserProvider;
import app.partsvibe.users.errors.CurrentUserMismatchException;
import app.partsvibe.users.errors.InvalidCurrentPasswordException;
import app.partsvibe.users.errors.PasswordsDoNotMatchException;
import app.partsvibe.users.errors.UserNotFoundException;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.security.password.PasswordPolicyValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class UpdatePasswordCommandHandler extends BaseCommandHandler<UpdatePasswordCommand, NoResult> {
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;
    private final PasswordEncoder passwordEncoder;

    UpdatePasswordCommandHandler(
            UserRepository userRepository, CurrentUserProvider currentUserProvider, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected NoResult doHandle(UpdatePasswordCommand command) {
        assertCurrentUserMatches(command.userId());

        if (!command.newPassword().equals(command.repeatedNewPassword())) {
            throw new PasswordsDoNotMatchException();
        }

        var user = userRepository
                .findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        if (!passwordEncoder.matches(command.currentPassword(), user.getPasswordHash())) {
            throw new InvalidCurrentPasswordException();
        }

        PasswordPolicyValidator.validate(command.newPassword(), user.getUsername());
        user.setPasswordHash(passwordEncoder.encode(command.newPassword()));
        userRepository.save(user);

        return NoResult.INSTANCE;
    }

    private void assertCurrentUserMatches(Long commandUserId) {
        Long currentUserId = currentUserProvider.currentUserId().orElse(null);
        if (currentUserId == null || !currentUserId.equals(commandUserId)) {
            throw new CurrentUserMismatchException(commandUserId, currentUserId);
        }
    }
}
