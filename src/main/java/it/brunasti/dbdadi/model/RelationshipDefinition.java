package it.brunasti.dbdadi.model;

import it.brunasti.dbdadi.model.enums.RelationshipType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "relationship_definitions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RelationshipDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationshipType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_table_id", nullable = false)
    private TableDefinition fromTable;

    @Column(name = "from_column_name", nullable = false)
    private String fromColumnName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_table_id", nullable = false)
    private TableDefinition toTable;

    @Column(name = "to_column_name", nullable = false)
    private String toColumnName;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
