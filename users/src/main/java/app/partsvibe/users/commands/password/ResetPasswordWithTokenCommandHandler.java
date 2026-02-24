package app.partsvibe.users.commands.password;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.cqrs.NoResult;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.users.domain.security.UserCredentialTokenPurpose;
import app.partsvibe.users.errors.InvalidOrExpiredCredentialTokenException;
import app.partsvibe.users.errors.WeakPasswordException;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.security.UserCredentialTokenRepository;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class ResetPasswordWithTokenCommandHandler extends BaseCommandHandler<ResetPasswordWithTokenCommand, NoResult> {
    private final UserCredentialTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final CredentialTokenCodec tokenCodec;
    private final PasswordEncoder passwordEncoder;
    private final TimeProvider timeProvider;

    ResetPasswordWithTokenCommandHandler(
            UserCredentialTokenRepository tokenRepository,
            UserRepository userRepository,
            CredentialTokenCodec tokenCodec,
            PasswordEncoder passwordEncoder,
            TimeProvider timeProvider) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.tokenCodec = tokenCodec;
        this.passwordEncoder = passwordEncoder;
        this.timeProvider = timeProvider;
    }

    @Override
    protected NoResult doHandle(ResetPasswordWithTokenCommand command) {
        if (!command.password().equals(command.repeatedPassword())) {
            throw new WeakPasswordException("Passwords do not match.");
        }

        String tokenHash = tokenCodec.hash(command.token().trim());
        var token =
                tokenRepository.findByTokenHash(tokenHash).orElseThrow(InvalidOrExpiredCredentialTokenException::new);

        var now = timeProvider.now();
        if (!token.isActiveAt(now)) {
            throw new InvalidOrExpiredCredentialTokenException();
        }

        var user = token.getUser();
        validatePasswordPolicy(command.password(), user.getUsername());

        user.setPasswordHash(passwordEncoder.encode(command.password()));
        userRepository.save(user);

        token.setUsedAt(now);
        tokenRepository.save(token);

        tokenRepository.revokeActiveTokensByUserAndPurpose(
                user.getId(), UserCredentialTokenPurpose.PASSWORD_RESET, now);
        tokenRepository.revokeActiveTokensByUserAndPurpose(
                user.getId(), UserCredentialTokenPurpose.INVITE_ACTIVATION, now);

        return NoResult.INSTANCE;
    }

    private static void validatePasswordPolicy(String password, String username) {
        if (password.length() < 12) {
            throw new WeakPasswordException("Password must have at least 12 characters.");
        }

        String normalizedPassword = password.toLowerCase(Locale.ROOT);
        String normalizedUsername = username.toLowerCase(Locale.ROOT);
        if (normalizedPassword.contains(normalizedUsername)) {
            throw new WeakPasswordException("Password cannot contain your username/email.");
        }
        int at = normalizedUsername.indexOf('@');
        if (at > 0 && normalizedPassword.contains(normalizedUsername.substring(0, at))) {
            throw new WeakPasswordException("Password cannot contain your username/email.");
        }
    }
}
