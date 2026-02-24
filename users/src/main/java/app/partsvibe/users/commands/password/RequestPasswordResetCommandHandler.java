package app.partsvibe.users.commands.password;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.cqrs.NoResult;
import app.partsvibe.shared.events.publishing.EventPublisher;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.users.domain.security.UserCredentialToken;
import app.partsvibe.users.domain.security.UserCredentialTokenPurpose;
import app.partsvibe.users.events.PasswordResetRequestedEvent;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.security.UserCredentialTokenRepository;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
class RequestPasswordResetCommandHandler extends BaseCommandHandler<RequestPasswordResetCommand, NoResult> {
    private static final long RESET_TOKEN_TTL_SECONDS = 24 * 3600L;

    private final UserRepository userRepository;
    private final UserCredentialTokenRepository tokenRepository;
    private final CredentialTokenCodec tokenCodec;
    private final EventPublisher eventPublisher;
    private final TimeProvider timeProvider;

    RequestPasswordResetCommandHandler(
            UserRepository userRepository,
            UserCredentialTokenRepository tokenRepository,
            CredentialTokenCodec tokenCodec,
            EventPublisher eventPublisher,
            TimeProvider timeProvider) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.tokenCodec = tokenCodec;
        this.eventPublisher = eventPublisher;
        this.timeProvider = timeProvider;
    }

    @Override
    protected NoResult doHandle(RequestPasswordResetCommand command) {
        String email = command.email().trim().toLowerCase(Locale.ROOT);

        var userOpt = userRepository.findByUsernameIgnoreCase(email);
        if (userOpt.isEmpty()) {
            return NoResult.INSTANCE;
        }

        var user = userOpt.get();
        Instant now = timeProvider.now();
        tokenRepository.revokeActiveTokensByUserAndPurpose(
                user.getId(), UserCredentialTokenPurpose.PASSWORD_RESET, now);

        Instant expiresAt = now.plusSeconds(RESET_TOKEN_TTL_SECONDS);
        String rawToken = tokenCodec.newRawToken();
        String hash = tokenCodec.hash(rawToken);
        tokenRepository.save(new UserCredentialToken(user, hash, UserCredentialTokenPurpose.PASSWORD_RESET, expiresAt));

        eventPublisher.publish(PasswordResetRequestedEvent.create(user.getUsername(), rawToken, expiresAt));

        return NoResult.INSTANCE;
    }
}
