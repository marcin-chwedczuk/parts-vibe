package app.partsvibe.users.commands.profile;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.cqrs.NoResult;
import app.partsvibe.shared.security.AuthorizationService;
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
    private final AuthorizationService authorizationService;
    private final PasswordEncoder passwordEncoder;

    UpdatePasswordCommandHandler(
            UserRepository userRepository, AuthorizationService authorizationService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected NoResult doHandle(UpdatePasswordCommand command) {
        authorizationService.assertCurrentUserHasId(command.userId(), CurrentUserMismatchException::new);

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
