package vibe.webapp.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vibe.webapp.domain.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
  Optional<UserAccount> findByUsername(String username);
}
