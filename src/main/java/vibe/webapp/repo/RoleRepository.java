package vibe.webapp.repo;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import vibe.webapp.domain.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
  Optional<Role> findByName(String name);
}
