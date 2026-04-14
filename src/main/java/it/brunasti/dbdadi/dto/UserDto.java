package it.brunasti.dbdadi.dto;

import it.brunasti.dbdadi.model.enums.UserRole;
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
public class UserDto {

    private Long id;

    @NotBlank
    private String username;

    // Only used on create/update — never returned in responses
    private String password;

    @NotNull
    private UserRole role;

    private boolean enabled;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
