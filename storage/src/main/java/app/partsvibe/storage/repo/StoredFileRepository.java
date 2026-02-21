package app.partsvibe.storage.repo;

import app.partsvibe.storage.domain.StoredFile;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {
    Optional<StoredFile> findByFileId(UUID fileId);

    boolean existsByFileId(UUID fileId);
}
