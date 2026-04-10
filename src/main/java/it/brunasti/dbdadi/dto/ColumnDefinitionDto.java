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
public class ColumnDefinitionDto {

    private Long id;

    @NotBlank
    private String name;

    private String description;

    @NotBlank
    private String dataType;

    private Integer length;
    private Integer precision;
    private Integer scale;
    private boolean nullable;
    private boolean primaryKey;
    private boolean unique;
    private String defaultValue;
    private Integer ordinalPosition;

    @NotNull
    private Long tableId;

    private String tableName;
    private Long schemaId;
    private String schemaName;
    private Long databaseModelId;
    private String databaseModelName;

    private Long attributeId;
    private String attributeName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
