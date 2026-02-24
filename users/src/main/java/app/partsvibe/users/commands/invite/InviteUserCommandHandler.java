package app.partsvibe.users.commands.invite;

import app.partsvibe.shared.cqrs.BaseCommandHandler;
import app.partsvibe.shared.events.publishing.EventPublisher;
import app.partsvibe.shared.time.TimeProvider;
import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.User;
import app.partsvibe.users.domain.security.UserCredentialToken;
import app.partsvibe.users.domain.security.UserCredentialTokenPurpose;
import app.partsvibe.users.errors.InvalidInviteRoleException;
import app.partsvibe.users.events.UserInvitedEvent;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserRepository;
import app.partsvibe.users.repo.security.UserCredentialTokenRepository;
import app.partsvibe.users.security.tokens.CredentialTokenCodec;
import java.time.Instant;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
class InviteUserCommandHandler extends BaseCommandHandler<InviteUserCommand, InviteUserCommandResult> {
    private static final Set<String> ALLOWED_ROLES = Set.of("ROLE_USER", "ROLE_ADMIN");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserCredentialTokenRepository tokenRepository;
    private final CredentialTokenCodec tokenCodec;
    private final EventPublisher eventPublisher;
    private final PasswordEncoder passwordEncoder;
    private final TimeProvider timeProvider;

    InviteUserCommandHandler(
            UserRepository userRepository,
            RoleRepository roleRepository,
            UserCredentialTokenRepository tokenRepository,
            CredentialTokenCodec tokenCodec,
            EventPublisher eventPublisher,
            PasswordEncoder passwordEncoder,
            TimeProvider timeProvider) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenRepository = tokenRepository;
        this.tokenCodec = tokenCodec;
        this.eventPublisher = eventPublisher;
        this.passwordEncoder = passwordEncoder;
        this.timeProvider = timeProvider;
    }

    @Override
    protected InviteUserCommandResult doHandle(InviteUserCommand command) {
        String email = canonicalizeEmail(command.email());
        String roleName = normalizeRole(command.roleName());
        String inviteMessage = command.inviteMessage();

        if (!ALLOWED_ROLES.contains(roleName)) {
            throw new InvalidInviteRoleException(roleName);
        }

        Role role = roleRepository.findByName(roleName).orElseGet(() -> roleRepository.save(new Role(roleName)));

        var existingOpt = userRepository.findByUsernameIgnoreCase(email);
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            if (!existing.isEnabled()) {
                return new InviteUserCommandResult(
                        existing.getId(),
                        existing.getUsername(),
                        null,
                        InviteUserCommandResult.InviteOutcome.ALREADY_ONBOARDED_LOCKED);
            }

            boolean hasInviteHistory = tokenRepository.existsByUserIdAndPurpose(
                    existing.getId(), UserCredentialTokenPurpose.INVITE_ACTIVATION);
            boolean usedInviteToken = tokenRepository.existsByUserIdAndPurposeAndUsedAtIsNotNull(
                    existing.getId(), UserCredentialTokenPurpose.INVITE_ACTIVATION);

            if (!hasInviteHistory || usedInviteToken) {
                return new InviteUserCommandResult(
                        existing.getId(),
                        existing.getUsername(),
                        null,
                        InviteUserCommandResult.InviteOutcome.ALREADY_ONBOARDED);
            }

            existing.getRoles().add(role);
            User saved = userRepository.save(existing);
            Instant expiresAt = publishInvite(saved, roleName, command.validityHours(), inviteMessage);
            return new InviteUserCommandResult(
                    saved.getId(), saved.getUsername(), expiresAt, InviteUserCommandResult.InviteOutcome.INVITE_RESENT);
        }

        User user = new User(email, passwordEncoder.encode(UUID.randomUUID().toString()));
        user.setEnabled(true);
        user.getRoles().add(role);
        User saved = userRepository.save(user);
        Instant expiresAt = publishInvite(saved, roleName, command.validityHours(), inviteMessage);
        return new InviteUserCommandResult(
                saved.getId(), saved.getUsername(), expiresAt, InviteUserCommandResult.InviteOutcome.INVITE_SENT);
    }

    private Instant publishInvite(User user, String roleName, int validityHours, String inviteMessage) {
        Instant now = timeProvider.now();
        tokenRepository.revokeActiveTokensByUserAndPurpose(
                user.getId(), UserCredentialTokenPurpose.INVITE_ACTIVATION, now);
        Instant expiresAt = now.plusSeconds(validityHours * 3600L);
        String rawToken = tokenCodec.newRawToken();
        String tokenHash = tokenCodec.hash(rawToken);

        UserCredentialToken token =
                new UserCredentialToken(user, tokenHash, UserCredentialTokenPurpose.INVITE_ACTIVATION, expiresAt);
        tokenRepository.save(token);

        eventPublisher.publish(
                UserInvitedEvent.create(user.getUsername(), rawToken, expiresAt, inviteMessage, roleName));
        return expiresAt;
    }

    private static String canonicalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeRole(String roleName) {
        return roleName.trim().toUpperCase(Locale.ROOT);
    }
}
