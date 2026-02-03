package app.partsvibe.users.service.impl;

import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.UserAccount;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserAccountRepository;
import app.partsvibe.users.service.UserProvisioningService;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProvisioningServiceImpl implements UserProvisioningService {
    private final RoleRepository roleRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProvisioningServiceImpl(
            RoleRepository roleRepository,
            UserAccountRepository userAccountRepository,
            PasswordEncoder passwordEncoder) {
        this.roleRepository = roleRepository;
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void seedUsers(String adminUsername, String adminPassword, String userUsername, String userPassword) {
        Role adminRole =
                roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));
        Role userRole =
                roleRepository.findByName("ROLE_USER").orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

        userAccountRepository.findByUsername(adminUsername).orElseGet(() -> {
            UserAccount admin = new UserAccount(adminUsername, passwordEncoder.encode(adminPassword));
            admin.setRoles(new HashSet<>(Set.of(adminRole, userRole)));
            return userAccountRepository.save(admin);
        });

        userAccountRepository.findByUsername(userUsername).orElseGet(() -> {
            UserAccount user = new UserAccount(userUsername, passwordEncoder.encode(userPassword));
            user.setRoles(new HashSet<>(Set.of(userRole)));
            return userAccountRepository.save(user);
        });
    }
}
