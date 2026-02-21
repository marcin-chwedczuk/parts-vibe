package app.partsvibe.storage.queries;

import app.partsvibe.shared.cqrs.BaseQueryHandler;
import app.partsvibe.storage.api.StorageFileVariant;
import app.partsvibe.storage.domain.QStoredFile;
import app.partsvibe.storage.domain.StoredFileStatus;
import app.partsvibe.storage.errors.StoredFileNotFoundException;
import app.partsvibe.storage.service.FilesystemStorage;
import app.partsvibe.storage.service.StoragePathResolver;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ResolveStoredFileQueryHandler
        extends BaseQueryHandler<ResolveStoredFileQuery, ResolveStoredFileQuery.StoredFileResource> {
    private static final String APPLICATION_OCTET_STREAM = "application/octet-stream";

    private final JPAQueryFactory queryFactory;
    private final StoragePathResolver pathResolver;
    private final FilesystemStorage filesystemStorage;

    ResolveStoredFileQueryHandler(
            JPAQueryFactory queryFactory, StoragePathResolver pathResolver, FilesystemStorage filesystemStorage) {
        this.queryFactory = queryFactory;
        this.pathResolver = pathResolver;
        this.filesystemStorage = filesystemStorage;
    }

    @Override
    protected ResolveStoredFileQuery.StoredFileResource doHandle(ResolveStoredFileQuery query) {
        QStoredFile storedFile = QStoredFile.storedFile;

        var projection = queryFactory
                .select(
                        storedFile.fileId,
                        storedFile.status,
                        storedFile.mimeType,
                        storedFile.kind,
                        storedFile.thumbnail128Ready,
                        storedFile.thumbnail512Ready,
                        storedFile.sizeBytes)
                .from(storedFile)
                .where(storedFile.fileId.eq(query.fileId()))
                .fetchOne();

        if (projection == null || projection.get(storedFile.status) != StoredFileStatus.READY) {
            throw new StoredFileNotFoundException(query.fileId());
        }

        Path targetPath = resolvePath(
                query.fileId(),
                query.variant(),
                Boolean.TRUE.equals(projection.get(storedFile.thumbnail128Ready)),
                Boolean.TRUE.equals(projection.get(storedFile.thumbnail512Ready)));

        if (!filesystemStorage.exists(targetPath)) {
            Path fallbackPath = pathResolver.blobPath(query.fileId());
            if (!filesystemStorage.exists(fallbackPath)) {
                throw new StoredFileNotFoundException(query.fileId());
            }
            targetPath = fallbackPath;
        }

        String mimeType = projection.get(storedFile.mimeType);
        long sizeBytes = fileSizeOf(targetPath);

        return new ResolveStoredFileQuery.StoredFileResource(
                targetPath, mimeType == null ? APPLICATION_OCTET_STREAM : mimeType, sizeBytes);
    }

    private Path resolvePath(
            UUID fileId, StorageFileVariant variant, boolean thumbnail128Ready, boolean thumbnail512Ready) {
        if (variant == StorageFileVariant.THUMBNAIL_128 && thumbnail128Ready) {
            return pathResolver.thumbnail128Path(fileId);
        }
        if (variant == StorageFileVariant.THUMBNAIL_512 && thumbnail512Ready) {
            return pathResolver.thumbnail512Path(fileId);
        }
        return pathResolver.blobPath(fileId);
    }

    private static long fileSizeOf(Path path) {
        try {
            return Files.size(path);
        } catch (java.io.IOException ex) {
            throw new app.partsvibe.shared.error.ApplicationException(
                    "Failed to read stored file size. path=" + path, ex);
        }
    }
}
