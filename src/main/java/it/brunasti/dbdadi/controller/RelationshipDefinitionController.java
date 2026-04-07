package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.dto.RelationshipDefinitionDto;
import it.brunasti.dbdadi.service.RelationshipDefinitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/relationships")
@RequiredArgsConstructor
@Tag(name = "Relationship Definitions", description = "Manage relationships between tables")
public class RelationshipDefinitionController {

    private final RelationshipDefinitionService service;

    @GetMapping
    @Operation(summary = "List all relationships, optionally filtered by table or database model")
    public List<RelationshipDefinitionDto> findAll(
            @RequestParam(required = false) Long fromTableId,
            @RequestParam(required = false) Long databaseModelId) {
        if (fromTableId != null) {
            return service.findByFromTable(fromTableId);
        }
        if (databaseModelId != null) {
            return service.findByDatabaseModel(databaseModelId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a relationship by ID")
    public RelationshipDefinitionDto findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new relationship definition")
    public RelationshipDefinitionDto create(@Valid @RequestBody RelationshipDefinitionDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a relationship definition")
    public RelationshipDefinitionDto update(@PathVariable Long id, @Valid @RequestBody RelationshipDefinitionDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a relationship definition")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
