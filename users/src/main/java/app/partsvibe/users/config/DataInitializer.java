package app.partsvibe.users.config;

import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import app.partsvibe.users.domain.Role;
import app.partsvibe.users.domain.UserAccount;
import app.partsvibe.users.repo.RoleRepository;
import app.partsvibe.users.repo.UserAccountRepository;

@Component
public class DataInitializer implements ApplicationRunner {
  private final RoleRepository roleRepository;
  private final UserAccountRepository userAccountRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.security.admin-username}")
  private String adminUsername;

  @Value("${app.security.admin-password}")
  private String adminPassword;

  @Value("${app.security.user-username}")
  private String userUsername;

  @Value("${app.security.user-password}")
  private String userPassword;

  public DataInitializer(RoleRepository roleRepository,
                         UserAccountRepository userAccountRepository,
                         PasswordEncoder passwordEncoder) {
    this.roleRepository = roleRepository;
    this.userAccountRepository = userAccountRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    Role adminRole = roleRepository.findByName("ROLE_ADMIN")
        .orElseGet(() -> roleRepository.save(new Role("ROLE_ADMIN")));
    Role userRole = roleRepository.findByName("ROLE_USER")
        .orElseGet(() -> roleRepository.save(new Role("ROLE_USER")));

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
