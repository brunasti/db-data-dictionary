package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.dto.DatabaseModelDto;
import it.brunasti.dbdadi.model.enums.DbType;
import it.brunasti.dbdadi.service.DatabaseModelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/database-models")
@RequiredArgsConstructor
@Tag(name = "Database Models", description = "Manage database model definitions")
public class DatabaseModelController {

    private final DatabaseModelService service;

    @GetMapping
    @Operation(summary = "List all database models")
    public List<DatabaseModelDto> findAll(
            @RequestParam(required = false) DbType dbType) {
        if (dbType != null) {
            return service.findByDbType(dbType);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a database model by ID")
    public DatabaseModelDto findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new database model")
    public DatabaseModelDto create(@Valid @RequestBody DatabaseModelDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a database model")
    public DatabaseModelDto update(@PathVariable Long id, @Valid @RequestBody DatabaseModelDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a database model")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
