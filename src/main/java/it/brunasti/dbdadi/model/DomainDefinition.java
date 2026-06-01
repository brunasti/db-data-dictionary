package it.brunasti.dbdadi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "domain_definitions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DomainDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @ManyToMany
    @JoinTable(
        name = "domain_entity",
        joinColumns = @JoinColumn(name = "domain_id"),
        inverseJoinColumns = @JoinColumn(name = "entity_id")
    )
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<EntityDefinition> entities = new ArrayList<>();

    @ManyToMany
    @JoinTable(
        name = "domain_database_model",
        joinColumns = @JoinColumn(name = "domain_id"),
        inverseJoinColumns = @JoinColumn(name = "database_model_id")
    )
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<DatabaseModel> databaseModels = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
