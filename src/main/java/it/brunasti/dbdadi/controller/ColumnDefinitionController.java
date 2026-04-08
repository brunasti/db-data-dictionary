package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.dto.ColumnDefinitionDto;
import it.brunasti.dbdadi.service.ColumnDefinitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/columns")
@RequiredArgsConstructor
@Tag(name = "Column Definitions", description = "Manage column definitions within a table")
public class ColumnDefinitionController {

    private final ColumnDefinitionService service;

    @GetMapping
    @Operation(summary = "List all columns, optionally filtered by tableId, schemaId or databaseModelId")
    public List<ColumnDefinitionDto> findAll(
            @RequestParam(required = false) Long tableId,
            @RequestParam(required = false) Long schemaId,
            @RequestParam(required = false) Long databaseModelId) {
        if (tableId != null) return service.findByTable(tableId);
        if (schemaId != null) return service.findBySchema(schemaId);
        if (databaseModelId != null) return service.findByDatabaseModel(databaseModelId);
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a column by ID")
    public ColumnDefinitionDto findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new column definition")
    public ColumnDefinitionDto create(@Valid @RequestBody ColumnDefinitionDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a column definition")
    public ColumnDefinitionDto update(@PathVariable Long id, @Valid @RequestBody ColumnDefinitionDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a column definition")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
