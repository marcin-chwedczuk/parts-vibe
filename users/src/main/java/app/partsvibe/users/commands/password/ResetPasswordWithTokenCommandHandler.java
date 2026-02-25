package app.partsvibe.users.commands.password;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.cqrs.NoResult;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.domain.invite.UserInvite;
import app.partsvibe.users.domain.security.UserPasswordResetToken;
import app.partsvibe.users.errors.InvalidOrExpiredCredentialTokenException;
import app.partsvibe.users.errors.WeakPasswordException;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.invite.UserInviteRepository;
import app.partsvibe.users.repo.security.UserPasswordResetTokenRepository;
import app.partsvibe.users.security.password.PasswordPolicyValidator;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class ResetPasswordWithTokenCommandHandler extends BaseCommandHandler<ResetPasswordWithTokenCommand, NoResult> {
    private final UserPasswordResetTokenRepository tokenRepository;
    private final UserInviteRepository userInviteRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CredentialTokenCodec tokenCodec;
    private final PasswordEncoder passwordEncoder;
    private final TimeProvider timeProvider;

    ResetPasswordWithTokenCommandHandler(
            UserPasswordResetTokenRepository tokenRepository,
            UserInviteRepository userInviteRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            CredentialTokenCodec tokenCodec,
            PasswordEncoder passwordEncoder,
            TimeProvider timeProvider) {
        this.tokenRepository = tokenRepository;
        this.userInviteRepository = userInviteRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
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
        var now = timeProvider.now();
        var credentialTokenOpt = tokenRepository.findByTokenHash(tokenHash);
        if (credentialTokenOpt.isPresent()) {
            return handlePasswordResetToken(credentialTokenOpt.get(), command.password(), now);
        }

        var inviteOpt = userInviteRepository.findByTokenHash(tokenHash);
        if (inviteOpt.isPresent()) {
            return handleInviteToken(inviteOpt.get(), command.password(), now);
        }

        throw new InvalidOrExpiredCredentialTokenException();
    }

    private NoResult handlePasswordResetToken(UserPasswordResetToken token, String newPassword, Instant now) {
        if (!token.isActiveAt(now)) {
            throw new InvalidOrExpiredCredentialTokenException();
        }

        var user = token.getUser();
        PasswordPolicyValidator.validate(newPassword, user.getUsername());

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        token.setUsedAt(now);
        tokenRepository.save(token);

        tokenRepository.revokeActiveTokensByUserId(user.getId(), now);
        return NoResult.INSTANCE;
    }

    private NoResult handleInviteToken(UserInvite invite, String newPassword, Instant now) {
        if (!invite.isActiveAt(now)) {
            throw new InvalidOrExpiredCredentialTokenException();
        }

        if (userRepository.findByUsernameIgnoreCase(invite.getEmail()).isPresent()) {
            invite.setRevokedAt(now);
            userInviteRepository.save(invite);
            throw new InvalidOrExpiredCredentialTokenException();
        }

        var role = roleRepository
                .findByName(invite.getRoleName())
                .orElseThrow(InvalidOrExpiredCredentialTokenException::new);

        PasswordPolicyValidator.validate(newPassword, invite.getEmail());

        User user = new User(invite.getEmail(), passwordEncoder.encode(newPassword));
        user.setEnabled(true);
        user.getRoles().add(role);
        userRepository.save(user);

        invite.setUsedAt(now);
        userInviteRepository.save(invite);

        userInviteRepository.revokeUnconsumedInvitesByEmailExcludingId(invite.getEmail(), invite.getId(), now);

        return NoResult.INSTANCE;
    }
}
