package it.brunasti.dbdadi.dto;

import it.brunasti.dbdadi.model.enums.RelationshipType;
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
public class RelationshipDefinitionDto {

    private Long id;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private RelationshipType type;

    @NotNull
    private Long fromTableId;

    private String fromTableName;

    @NotBlank
    private String fromColumnName;

    private Long fromColumnId;

    @NotNull
    private Long toTableId;

    private String toTableName;

    @NotBlank
    private String toColumnName;

    private Long toColumnId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
