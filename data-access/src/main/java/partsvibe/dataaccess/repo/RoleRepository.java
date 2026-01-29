package partsvibe.dataaccess.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import partsvibe.dataaccess.domain.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByName(String name);
}
