package app.partsvibe.users.commands.profile;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.cqrs.NoResult;
import app.partsvibe.users.errors.UserNotFoundException;
import app.partsvibe.users.repo.UserRepository;
import org.springframework.stereotype.Component;

@Component
class UpdateCurrentUserProfileCommandHandler extends BaseCommandHandler<UpdateCurrentUserProfileCommand, NoResult> {
    private final UserRepository userRepository;

    UpdateCurrentUserProfileCommandHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected NoResult doHandle(UpdateCurrentUserProfileCommand command) {
        var user = userRepository
                .findByUsername(command.username())
                .orElseThrow(() -> new UserNotFoundException(command.username()));

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
