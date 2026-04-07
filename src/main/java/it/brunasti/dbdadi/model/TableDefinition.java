package it.brunasti.dbdadi.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "table_definitions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"database_model_id", "schema_name", "name"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(name = "schema_name")
    private String schemaName;

    @Column(length = 1000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "database_model_id", nullable = false)
    private DatabaseModel databaseModel;

    @OneToMany(mappedBy = "table", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ColumnDefinition> columns = new ArrayList<>();

    @OneToMany(mappedBy = "fromTable", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RelationshipDefinition> outgoingRelationships = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
