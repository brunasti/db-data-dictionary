package it.brunasti.dbdadi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttributeDefinitionDto {

    private Long id;

    @NotBlank
    private String name;

    private String description;

    private Long entityId;
    private String entityName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
