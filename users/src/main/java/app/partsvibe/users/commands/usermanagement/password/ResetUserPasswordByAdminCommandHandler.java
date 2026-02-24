package app.partsvibe.users.commands.usermanagement.password;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.errors.AdminPrivilegesRequiredException;
import app.partsvibe.users.errors.AdminReauthenticationFailedException;
import app.partsvibe.users.errors.UserNotFoundException;
import app.partsvibe.users.errors.WeakPasswordException;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.security.password.PasswordPolicyValidator;
import java.security.SecureRandom;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class ResetUserPasswordByAdminCommandHandler
        extends BaseCommandHandler<ResetUserPasswordByAdminCommand, ResetUserPasswordByAdminCommandResult> {
    private static final String TEMP_PASSWORD_CHARS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%^&*()-_=+";
    private static final int TEMP_PASSWORD_LENGTH = 20;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    ResetUserPasswordByAdminCommandHandler(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    protected ResetUserPasswordByAdminCommandResult doHandle(ResetUserPasswordByAdminCommand command) {
        User admin = userRepository
                .findById(command.adminUserId())
                .orElseThrow(() -> new UserNotFoundException(command.adminUserId()));

        boolean isAdmin = admin.getRoles().stream().anyMatch(role -> "ROLE_ADMIN".equals(role.getName()));
        if (!isAdmin) {
            throw new AdminPrivilegesRequiredException(command.adminUserId());
        }

        if (!passwordEncoder.matches(command.adminPassword(), admin.getPasswordHash())) {
            throw new AdminReauthenticationFailedException();
        }

        User targetUser = userRepository
                .findById(command.targetUserId())
                .orElseThrow(() -> new UserNotFoundException(command.targetUserId()));

        String temporaryPassword = generateTemporaryPasswordFor(targetUser);
        targetUser.setPasswordHash(passwordEncoder.encode(temporaryPassword));
        userRepository.save(targetUser);

        return new ResetUserPasswordByAdminCommandResult(
                targetUser.getId(), targetUser.getUsername(), temporaryPassword);
    }

    private String generateTemporaryPasswordFor(User targetUser) {
        for (int i = 0; i < 128; i++) {
            String password = randomPassword(TEMP_PASSWORD_LENGTH);
            try {
                PasswordPolicyValidator.validate(password, targetUser.getUsername());
                return password;
            } catch (WeakPasswordException ignored) {
                // Retry until we get a policy-compliant random password.
            }
        }

        throw new IllegalStateException("Failed to generate a valid temporary password.");
    }

    private String randomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = secureRandom.nextInt(TEMP_PASSWORD_CHARS.length());
            sb.append(TEMP_PASSWORD_CHARS.charAt(idx));
        }
        return sb.toString();
    }
}
