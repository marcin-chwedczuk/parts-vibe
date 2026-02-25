package app.partsvibe.users.commands.profile;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.cqrs.NoResult;
import app.partsvibe.shared.security.CurrentUserProvider;
import app.partsvibe.users.errors.CurrentUserMismatchException;
import app.partsvibe.users.errors.UserNotFoundException;
import app.partsvibe.users.repo.UserRepository;
import org.springframework.stereotype.Component;

@Component
class UpdateProfileCommandHandler extends BaseCommandHandler<UpdateProfileCommand, NoResult> {
    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    UpdateProfileCommandHandler(UserRepository userRepository, CurrentUserProvider currentUserProvider) {
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    protected NoResult doHandle(UpdateProfileCommand command) {
        assertCurrentUserMatches(command.userId());

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

    private void assertCurrentUserMatches(Long commandUserId) {
        Long currentUserId = currentUserProvider.currentUserId().orElse(null);
        if (currentUserId == null || !currentUserId.equals(commandUserId)) {
            throw new CurrentUserMismatchException(commandUserId, currentUserId);
        }
    }
}
