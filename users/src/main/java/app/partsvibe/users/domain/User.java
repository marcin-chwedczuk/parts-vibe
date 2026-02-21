package app.partsvibe.users.domain;

import app.partsvibe.shared.persistence.BaseAuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "users")
@SequenceGenerator(
        name = BaseAuditableEntity.ID_GENERATOR_NAME,
        sequenceName = "users_id_seq",
        allocationSize = BaseAuditableEntity.ID_ALLOCATION_SIZE)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseAuditableEntity {
    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean enabled = true;

    @Size(max = 1000)
    @Column(length = 1000)
    private String bio;

    @Size(max = 255)
    @Column(length = 255)
    private String website;

    @Column(name = "avatar_id")
    private UUID avatarId;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Setter(AccessLevel.NONE)
    private Set<Role> roles = new HashSet<>();

    public User(String username, String passwordHash) {
        this.username = username;
        this.passwordHash = passwordHash;
    }
}
