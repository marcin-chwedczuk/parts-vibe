package app.partsvibe.users.commands.profile;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.cqrs.NoResult;
import app.partsvibe.shared.security.AuthorizationService;
import app.partsvibe.users.errors.CurrentUserMismatchException;
import app.partsvibe.users.errors.UserNotFoundException;
import app.partsvibe.users.repo.UserRepository;
import org.springframework.stereotype.Component;

@Component
class UpdateProfileCommandHandler extends BaseCommandHandler<UpdateProfileCommand, NoResult> {
    private final UserRepository userRepository;
    private final AuthorizationService authorizationService;

    UpdateProfileCommandHandler(UserRepository userRepository, AuthorizationService authorizationService) {
        this.userRepository = userRepository;
        this.authorizationService = authorizationService;
    }

    @Override
    protected NoResult doHandle(UpdateProfileCommand command) {
        authorizationService.assertCurrentUserHasId(command.userId(), CurrentUserMismatchException::new);

        var user = userRepository
                .findById(command.userId())
                .orElseThrow(() -> new UserNotFoundException(command.userId()));

        user.setBio(normalizeNullable(command.bio()));
        user.setWebsite(normalizeNullable(command.website()));
        userRepository.save(user);
        return NoResult.INSTANCE;
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
