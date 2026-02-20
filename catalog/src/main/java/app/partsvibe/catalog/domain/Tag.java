package app.partsvibe.catalog.domain;

import app.partsvibe.shared.persistence.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "catalog_tags",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_catalog_tags_name_normalized", columnNames = "name_normalized")
        })
@SequenceGenerator(
        name = BaseAuditableEntity.ID_GENERATOR_NAME,
        sequenceName = "catalog_tags_id_seq",
        allocationSize = BaseAuditableEntity.ID_ALLOCATION_SIZE)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag extends BaseAuditableEntity {
    @NotBlank
    @Size(max = 64)
    @Pattern(regexp = "^[A-Za-z0-9-]+$")
    @Column(name = "name", nullable = false, length = 64)
    private String name;

    @Column(name = "name_normalized", nullable = false, length = 64)
    @Setter(AccessLevel.NONE)
    private String nameNormalized;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false, length = 16)
    private TagColor color;

    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    private String description;

    public Tag(String name, TagColor color, String description) {
        this.name = name;
        this.color = color;
        this.description = description;
    }

    @PrePersist
    @PreUpdate
    void normalizeName() {
        if (name != null) {
            name = name.trim();
            nameNormalized = name.toLowerCase(Locale.ROOT);
        }
    }
}
