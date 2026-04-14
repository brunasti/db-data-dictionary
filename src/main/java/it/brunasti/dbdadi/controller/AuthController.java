package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.dto.LoginRequest;
import it.brunasti.dbdadi.dto.UserDto;
import it.brunasti.dbdadi.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Authentication")
public class AuthController {

    private final UserService service;

    @PostMapping("/login")
    @Operation(summary = "Validate credentials and return user info with role")
    public ResponseEntity<UserDto> login(@RequestBody LoginRequest request) {
        if (service.validateCredentials(request.getUsername(), request.getPassword())) {
            UserDto dto = service.findRawByUsername(request.getUsername())
                    .map(u -> UserDto.builder()
                            .id(u.getId())
                            .username(u.getUsername())
                            .role(u.getRole())
                            .enabled(u.isEnabled())
                            .build())
                    .orElse(null);
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
}
