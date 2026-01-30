package app.partsvibe.users.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import app.partsvibe.users.domain.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByName(String name);
}
