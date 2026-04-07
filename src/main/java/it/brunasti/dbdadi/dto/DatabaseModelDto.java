package it.brunasti.dbdadi.dto;

import it.brunasti.dbdadi.model.enums.DbType;
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
public class DatabaseModelDto {

    private Long id;

    @NotBlank
    private String name;

    private String description;

    @NotNull
    private DbType dbType;

    private String version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
