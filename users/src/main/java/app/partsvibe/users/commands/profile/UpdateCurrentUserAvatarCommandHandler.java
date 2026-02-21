package app.partsvibe.users.commands.profile;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.users.errors.UserNotFoundException;
import app.partsvibe.users.repo.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class UpdateCurrentUserAvatarCommandHandler
        extends BaseCommandHandler<UpdateCurrentUserAvatarCommand, UpdateCurrentUserAvatarCommandResult> {
    private final UserRepository userRepository;

    UpdateCurrentUserAvatarCommandHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected UpdateCurrentUserAvatarCommandResult doHandle(UpdateCurrentUserAvatarCommand command) {
        var user = userRepository
                .findByUsername(command.username())
                .orElseThrow(() -> new UserNotFoundException(command.username()));

        UUID previousAvatarId = user.getAvatarId();
        user.setAvatarId(command.avatarId());
        userRepository.save(user);

        return new UpdateCurrentUserAvatarCommandResult(previousAvatarId, command.avatarId());
    }
}
