package it.brunasti.dbdadi.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TableDefinitionDto {

    private Long id;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private Long schemaId;

    private String schemaName;

    private Long databaseModelId;
    private String databaseModelName;

    private Long entityId;
    private String entityName;

    private Long rowCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
