package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.dto.TableDefinitionDto;
import it.brunasti.dbdadi.service.TableDefinitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tables")
@RequiredArgsConstructor
@Tag(name = "Table Definitions", description = "Manage table definitions within a database model")
public class TableDefinitionController {

    private final TableDefinitionService service;

    @GetMapping
    @Operation(summary = "List all tables, optionally filtered by database model")
    public List<TableDefinitionDto> findAll(
            @RequestParam(required = false) Long databaseModelId) {
        if (databaseModelId != null) {
            return service.findByDatabaseModel(databaseModelId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a table by ID")
    public TableDefinitionDto findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new table definition")
    public TableDefinitionDto create(@Valid @RequestBody TableDefinitionDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a table definition")
    public TableDefinitionDto update(@PathVariable Long id, @Valid @RequestBody TableDefinitionDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a table definition")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
