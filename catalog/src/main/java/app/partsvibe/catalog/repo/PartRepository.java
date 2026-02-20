package app.partsvibe.catalog.repo;

import app.partsvibe.catalog.domain.Part;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PartRepository extends JpaRepository<Part, Long> {
    Optional<Part> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    boolean existsByCategoryId(Long categoryId);

    boolean existsByTagsId(Long tagId);
}
