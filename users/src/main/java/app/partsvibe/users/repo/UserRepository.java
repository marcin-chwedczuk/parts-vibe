package app.partsvibe.users.repo;

import app.partsvibe.users.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    boolean existsByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCaseAndIdNot(String username, Long id);

    @Query(
            """
            select count(distinct u.id)
            from User u
            join u.roles r
            where u.enabled = true and r.name = :roleName
            """)
    long countActiveUsersByRoleName(@Param("roleName") String roleName);
}
