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

    private String schemaName;

    private String description;

    @NotNull
    private Long databaseModelId;

    private String databaseModelName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
