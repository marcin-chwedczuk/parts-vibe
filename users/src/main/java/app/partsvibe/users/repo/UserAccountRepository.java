package app.partsvibe.users.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import app.partsvibe.users.domain.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
  Optional<UserAccount> findByUsername(String username);
}
