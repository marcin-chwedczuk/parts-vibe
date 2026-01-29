package partsvibe.dataaccess.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import partsvibe.dataaccess.domain.UserAccount;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {
  Optional<UserAccount> findByUsername(String username);
}
