package app.partsvibe.users.commands.usermanagement;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.security.CurrentUserProvider;
import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.RoleNames;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.avatar.UserAvatarChangeRequestRepository;
import app.partsvibe.users.repo.security.UserPasswordResetTokenRepository;
import org.springframework.stereotype.Component;

@Component
class DeleteUserCommandHandler extends BaseCommandHandler<DeleteUserCommand, DeleteUserCommandResult> {
    private final UserRepository userRepository;
    private final UserAvatarChangeRequestRepository userAvatarChangeRequestRepository;
    private final UserPasswordResetTokenRepository userPasswordResetTokenRepository;
    private final CurrentUserProvider currentUserProvider;

    DeleteUserCommandHandler(
            UserRepository userRepository,
            UserAvatarChangeRequestRepository userAvatarChangeRequestRepository,
            UserPasswordResetTokenRepository userPasswordResetTokenRepository,
            CurrentUserProvider currentUserProvider) {
        this.userRepository = userRepository;
        this.userAvatarChangeRequestRepository = userAvatarChangeRequestRepository;
        this.userPasswordResetTokenRepository = userPasswordResetTokenRepository;
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
            long activeAdminUsersCount = userRepository.countActiveUsersByRoleName(RoleNames.ADMIN);
            if (activeAdminUsersCount <= 1) {
                throw new CannotDeleteLastActiveAdminException();
            }
        }

        userAvatarChangeRequestRepository.deleteByUserId(user.getId());
        userPasswordResetTokenRepository.deleteByUserId(user.getId());
        userRepository.delete(user);
        log.info("User deleted. userId={}, username={}", user.getId(), user.getUsername());
        return new DeleteUserCommandResult(user.getUsername());
    }

    private boolean hasAdminRole(User user) {
        return user.getRoles().stream().map(Role::getName).anyMatch(RoleNames.ADMIN::equals);
    }
}
