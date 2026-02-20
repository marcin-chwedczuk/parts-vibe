package app.partsvibe.catalog.domain;

import app.partsvibe.shared.persistence.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "catalog_parts",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_catalog_parts_name_normalized", columnNames = "name_normalized")
        },
        indexes = {@Index(name = "idx_catalog_parts_category_id", columnList = "category_id")})
@SequenceGenerator(
        name = BaseAuditableEntity.ID_GENERATOR_NAME,
        sequenceName = "catalog_parts_id_seq",
        allocationSize = BaseAuditableEntity.ID_ALLOCATION_SIZE)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Part extends BaseAuditableEntity {
    @NotBlank
    @Size(max = 256)
    @Column(name = "name", nullable = false, length = 256)
    private String name;

    @Column(name = "name_normalized", nullable = false, length = 256)
    @Setter(AccessLevel.NONE)
    private String nameNormalized;

    @NotBlank
    @Size(max = 10240)
    @Column(name = "description", nullable = false, length = 10240)
    private String description;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "catalog_part_tags",
            joinColumns = @JoinColumn(name = "part_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"),
            uniqueConstraints =
                    @UniqueConstraint(
                            name = "uk_catalog_part_tags_part_id_tag_id",
                            columnNames = {"part_id", "tag_id"}))
    @Setter(AccessLevel.NONE)
    private Set<Tag> tags = new HashSet<>();

    public Part(String name, String description, Category category) {
        this.name = name;
        this.description = description;
        this.category = category;
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
