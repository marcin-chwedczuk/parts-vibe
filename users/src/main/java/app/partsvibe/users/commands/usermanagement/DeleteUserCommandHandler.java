package app.partsvibe.users.commands.usermanagement;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.security.CurrentUserProvider;
import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.repo.UserRepository;
import org.springframework.stereotype.Component;

@Component
class DeleteUserCommandHandler extends BaseCommandHandler<DeleteUserCommand, DeleteUserCommandResult> {
    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final UserRepository userRepository;
    private final CurrentUserProvider currentUserProvider;

    DeleteUserCommandHandler(UserRepository userRepository, CurrentUserProvider currentUserProvider) {
        this.userRepository = userRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    protected DeleteUserCommandResult doHandle(DeleteUserCommand command) {
        User user = userRepository.findById(command.userId()).orElse(null);
        if (user == null) {
            log.info("Delete user requested for non-existing user. userId={}", command.userId());
            return new DeleteUserCommandResult(null);
        }

        Long currentUserId = currentUserProvider.currentUserId().orElse(null);
        if (currentUserId != null && currentUserId.equals(user.getId())) {
            throw new CannotDeleteCurrentUserException();
        }

        if (user.isEnabled() && hasAdminRole(user)) {
            long activeAdminUsersCount = userRepository.countActiveUsersByRoleName(ROLE_ADMIN);
            if (activeAdminUsersCount <= 1) {
                throw new CannotDeleteLastActiveAdminException();
            }
        }

        userRepository.delete(user);
        log.info("User deleted. userId={}, username={}", user.getId(), user.getUsername());
        return new DeleteUserCommandResult(user.getUsername());
    }

    private boolean hasAdminRole(User user) {
        return user.getRoles().stream().map(Role::getName).anyMatch(ROLE_ADMIN::equals);
    }
}
