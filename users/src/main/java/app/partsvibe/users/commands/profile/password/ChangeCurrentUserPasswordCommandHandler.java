package app.partsvibe.users.commands.profile.password;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.cqrs.NoResult;
import app.partsvibe.users.errors.InvalidCurrentPasswordException;
import app.partsvibe.users.errors.PasswordsDoNotMatchException;
import app.partsvibe.users.errors.UserNotFoundException;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.security.password.PasswordPolicyValidator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class ChangeCurrentUserPasswordCommandHandler extends BaseCommandHandler<ChangeCurrentUserPasswordCommand, NoResult> {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    ChangeCurrentUserPasswordCommandHandler(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected NoResult doHandle(ChangeCurrentUserPasswordCommand command) {
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
}
