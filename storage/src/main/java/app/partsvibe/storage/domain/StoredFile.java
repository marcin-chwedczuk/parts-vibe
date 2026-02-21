package app.partsvibe.storage.domain;

import app.partsvibe.shared.persistence.BaseAuditableEntity;
import app.partsvibe.storage.api.StorageObjectType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "stored_files",
        uniqueConstraints = {@UniqueConstraint(name = "uk_stored_files_file_id", columnNames = "file_id")})
@SequenceGenerator(
        name = BaseAuditableEntity.ID_GENERATOR_NAME,
        sequenceName = "stored_files_id_seq",
        allocationSize = BaseAuditableEntity.ID_ALLOCATION_SIZE)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoredFile extends BaseAuditableEntity {
    @NotNull
    @Column(name = "file_id", nullable = false, updatable = false)
    private UUID fileId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "object_type", nullable = false, updatable = false, length = 32)
    private StorageObjectType objectType;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, updatable = false, length = 16)
    private StoredFileKind kind;

    @NotBlank
    @Size(max = 256)
    @Column(name = "original_filename", nullable = false, updatable = false, length = 256)
    private String originalFilename;

    @NotNull
    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private StoredFileStatus status;

    @Size(max = 128)
    @Column(name = "mime_type", length = 128)
    private String mimeType;

    @NotNull
    @Column(name = "uploaded_at", nullable = false, updatable = false, columnDefinition = "timestamp with time zone")
    private Instant uploadedAt;

    @NotBlank
    @Size(max = 64)
    @Column(name = "uploaded_by", nullable = false, updatable = false, length = 64)
    private String uploadedBy;

    @Column(name = "scanned_at", columnDefinition = "timestamp with time zone")
    private Instant scannedAt;

    @Column(name = "deleted_at", columnDefinition = "timestamp with time zone")
    private Instant deletedAt;

    @Column(name = "thumbnail_128_ready", nullable = false)
    private boolean thumbnail128Ready;

    @Column(name = "thumbnail_512_ready", nullable = false)
    private boolean thumbnail512Ready;

    public StoredFile(
            UUID fileId,
            StorageObjectType objectType,
            StoredFileKind kind,
            String originalFilename,
            long sizeBytes,
            Instant uploadedAt,
            String uploadedBy) {
        this.fileId = fileId;
        this.objectType = objectType;
        this.kind = kind;
        this.originalFilename = originalFilename;
        this.sizeBytes = sizeBytes;
        this.uploadedAt = uploadedAt;
        this.uploadedBy = uploadedBy;
        this.status = StoredFileStatus.PENDING_SCAN;
        this.thumbnail128Ready = false;
        this.thumbnail512Ready = false;
    }

    @PrePersist
    void ensureDefaults() {
        if (status == null) {
            status = StoredFileStatus.PENDING_SCAN;
        }
    }
}
