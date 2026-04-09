package it.brunasti.dbdadi.model;

import it.brunasti.dbdadi.model.enums.DbType;
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
@Table(name = "database_models")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatabaseModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DbType dbType;

    private String version;

    @Column(length = 500)
    private String jdbcUrl;

    private String username;

    private String schemaPattern;

    private String tablePattern;

    @Column(length = 500)
    private String importFlags;

    @OneToMany(mappedBy = "databaseModel", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SchemaDefinition> schemas = new ArrayList<>();

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
