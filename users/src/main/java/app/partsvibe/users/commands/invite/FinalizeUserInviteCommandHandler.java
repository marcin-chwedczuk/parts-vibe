package app.partsvibe.users.commands.invite;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.cqrs.NoResult;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.errors.InvalidOrExpiredCredentialTokenException;
import app.partsvibe.users.errors.PasswordsDoNotMatchException;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.invite.UserInviteRepository;
import app.partsvibe.users.security.password.PasswordPolicyValidator;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import java.time.Instant;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class FinalizeUserInviteCommandHandler extends BaseCommandHandler<FinalizeUserInviteCommand, NoResult> {
    private final UserInviteRepository userInviteRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CredentialTokenCodec tokenCodec;
    private final PasswordEncoder passwordEncoder;
    private final TimeProvider timeProvider;

    FinalizeUserInviteCommandHandler(
            UserInviteRepository userInviteRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            CredentialTokenCodec tokenCodec,
            PasswordEncoder passwordEncoder,
            TimeProvider timeProvider) {
        this.userInviteRepository = userInviteRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenCodec = tokenCodec;
        this.passwordEncoder = passwordEncoder;
        this.timeProvider = timeProvider;
    }

    @Override
    protected NoResult doHandle(FinalizeUserInviteCommand command) {
        if (!command.password().equals(command.repeatedPassword())) {
            throw new PasswordsDoNotMatchException();
        }

        String tokenHash = tokenCodec.hash(command.token().trim());
        var invite = userInviteRepository
                .findByTokenHash(tokenHash)
                .orElseThrow(InvalidOrExpiredCredentialTokenException::new);
        Instant now = timeProvider.now();
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

        PasswordPolicyValidator.validate(command.password(), invite.getEmail());

        User user = new User(invite.getEmail(), passwordEncoder.encode(command.password()));
        user.setEnabled(true);
        user.getRoles().add(role);
        userRepository.save(user);

        invite.setUsedAt(now);
        userInviteRepository.save(invite);
        userInviteRepository.revokeUnconsumedInvitesByEmailExcludingId(invite.getEmail(), invite.getId(), now);

        return NoResult.INSTANCE;
    }
}
