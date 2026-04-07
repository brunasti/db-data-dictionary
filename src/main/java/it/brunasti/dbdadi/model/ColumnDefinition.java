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

@Entity
@Table(name = "column_definitions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"table_id", "name"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ColumnDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String description;

    @NotBlank
    @Column(nullable = false)
    private String dataType;

    private Integer length;
    private Integer precision;
    private Integer scale;

    @Column(nullable = false)
    @Builder.Default
    private boolean nullable = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean primaryKey = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean unique = false;

    private String defaultValue;

    private Integer ordinalPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private TableDefinition table;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
