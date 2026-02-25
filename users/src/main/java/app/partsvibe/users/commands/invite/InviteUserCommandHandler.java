package app.partsvibe.users.commands.invite;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.events.publishing.EventPublisher;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.domain.invite.UserInvite;
import app.partsvibe.users.errors.InvalidInviteRoleException;
import app.partsvibe.users.events.UserInvitedEvent;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.invite.UserInviteRepository;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
class InviteUserCommandHandler extends BaseCommandHandler<InviteUserCommand, InviteUserCommandResult> {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserInviteRepository userInviteRepository;
    private final CredentialTokenCodec tokenCodec;
    private final EventPublisher eventPublisher;
    private final TimeProvider timeProvider;

    InviteUserCommandHandler(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserInviteRepository userInviteRepository,
            CredentialTokenCodec tokenCodec,
            EventPublisher eventPublisher,
            TimeProvider timeProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userInviteRepository = userInviteRepository;
        this.tokenCodec = tokenCodec;
        this.eventPublisher = eventPublisher;
        this.timeProvider = timeProvider;
    }

    @Override
    protected InviteUserCommandResult doHandle(InviteUserCommand command) {
        String email = canonicalizeEmail(command.email());
        String roleName = normalizeRole(command.roleName());
        String inviteMessage = command.inviteMessage();

        Role role = roleRepository.findByName(roleName).orElseThrow(() -> new InvalidInviteRoleException(roleName));

        var existingOpt = userRepository.findByUsernameIgnoreCase(email);
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            if (!existing.isEnabled()) {
                return new InviteUserCommandResult(
                        existing.getUsername(), null, InviteUserCommandResult.InviteOutcome.ALREADY_ONBOARDED_LOCKED);
            }
            return new InviteUserCommandResult(
                    existing.getUsername(), null, InviteUserCommandResult.InviteOutcome.ALREADY_ONBOARDED);
        }

        boolean hasInviteHistory = userInviteRepository.existsByEmailIgnoreCase(email);
        Instant expiresAt = publishInvite(email, role.getName(), command.validityHours(), inviteMessage);
        return new InviteUserCommandResult(
                email,
                expiresAt,
                hasInviteHistory
                        ? InviteUserCommandResult.InviteOutcome.INVITE_RESENT
                        : InviteUserCommandResult.InviteOutcome.INVITE_SENT);
    }

    private Instant publishInvite(String email, String roleName, int validityHours, String inviteMessage) {
        Instant now = timeProvider.now();
        userInviteRepository.revokeUnconsumedInvitesByEmail(email, now);
        Instant expiresAt = now.plusSeconds(validityHours * 3600L);
        String rawToken = tokenCodec.newRawToken();
        String tokenHash = tokenCodec.hash(rawToken);

        userInviteRepository.save(new UserInvite(email, roleName, inviteMessage, tokenHash, expiresAt));

        eventPublisher.publish(UserInvitedEvent.create(email, rawToken, expiresAt, inviteMessage, roleName));
        return expiresAt;
    }

    private static String canonicalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeRole(String roleName) {
        return roleName.trim().toUpperCase(Locale.ROOT);
    }
}
