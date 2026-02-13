package app.partsvibe.shared.persistence;

import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseVersionableEntity extends BaseEntity {
    @Version
    private Long version;
}
