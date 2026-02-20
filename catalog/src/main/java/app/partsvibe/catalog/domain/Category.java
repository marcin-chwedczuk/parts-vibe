package app.partsvibe.catalog.domain;

import app.partsvibe.shared.persistence.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "catalog_categories",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_catalog_categories_name_normalized", columnNames = "name_normalized")
        })
@SequenceGenerator(
        name = BaseAuditableEntity.ID_GENERATOR_NAME,
        sequenceName = "catalog_categories_id_seq",
        allocationSize = BaseAuditableEntity.ID_ALLOCATION_SIZE)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseAuditableEntity {
    @NotBlank
    @Size(max = 128)
    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "name_normalized", nullable = false, length = 128)
    @Setter(AccessLevel.NONE)
    private String nameNormalized;

    @Size(max = 1000)
    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "picture_id")
    private UUID pictureId;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "catalog_category_tags",
            joinColumns = @JoinColumn(name = "category_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"),
            uniqueConstraints =
                    @UniqueConstraint(
                            name = "uk_catalog_category_tags_category_id_tag_id",
                            columnNames = {"category_id", "tag_id"}))
    @Setter(AccessLevel.NONE)
    private Set<Tag> tags = new HashSet<>();

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    @Setter(AccessLevel.NONE)
    private Set<Part> parts = new HashSet<>();

    public Category(String name, String description, UUID pictureId) {
        this.name = name;
        this.description = description;
        this.pictureId = pictureId;
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
