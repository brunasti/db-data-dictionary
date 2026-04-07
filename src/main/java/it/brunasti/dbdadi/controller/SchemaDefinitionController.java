package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.dto.SchemaDefinitionDto;
import it.brunasti.dbdadi.service.SchemaDefinitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/schemas")
@RequiredArgsConstructor
@Tag(name = "Schema Definitions", description = "Manage schema definitions within a database model")
public class SchemaDefinitionController {

    private final SchemaDefinitionService service;

    @GetMapping
    @Operation(summary = "List all schemas, optionally filtered by database model")
    public List<SchemaDefinitionDto> findAll(
            @RequestParam(required = false) Long databaseModelId) {
        if (databaseModelId != null) {
            return service.findByDatabaseModel(databaseModelId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a schema by ID")
    public SchemaDefinitionDto findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new schema definition")
    public SchemaDefinitionDto create(@Valid @RequestBody SchemaDefinitionDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a schema definition")
    public SchemaDefinitionDto update(@PathVariable Long id, @Valid @RequestBody SchemaDefinitionDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a schema definition")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
