package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.aspect.Loggable;
import it.brunasti.dbdadi.dto.DatabaseModelDto;
import it.brunasti.dbdadi.dto.DomainDefinitionDto;
import it.brunasti.dbdadi.dto.EntityDefinitionDto;
import it.brunasti.dbdadi.dto.GenerateAttributesResult;
import it.brunasti.dbdadi.service.DomainDefinitionService;
import it.brunasti.dbdadi.service.GenerateAttributesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/domains")
@RequiredArgsConstructor
@Tag(name = "Domain Definitions", description = "Manage domain definitions that aggregate logical entities")
public class DomainDefinitionController {

    private final DomainDefinitionService service;
    private final GenerateAttributesService generateAttributesService;

    @GetMapping
    @Operation(summary = "List all domains, optionally filtered by entity or database model")
    @Loggable
    public List<DomainDefinitionDto> findAll(
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) Long databaseModelId) {
        if (entityId != null) return service.findByEntity(entityId);
        if (databaseModelId != null) return service.findByDatabaseModel(databaseModelId);
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a domain by ID")
    @Loggable
    public DomainDefinitionDto findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/{id}/entities")
    @Operation(summary = "Get all entities linked to a domain")
    @Loggable
    public List<EntityDefinitionDto> findEntities(@PathVariable Long id) {
        return service.findEntities(id);
    }

    @GetMapping("/{id}/database-models")
    @Operation(summary = "Get all database models linked to a domain")
    @Loggable
    public List<DatabaseModelDto> findDatabaseModels(@PathVariable Long id) {
        return service.findDatabaseModels(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new domain definition")
    @Loggable
    public DomainDefinitionDto create(@Valid @RequestBody DomainDefinitionDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a domain definition")
    @Loggable
    public DomainDefinitionDto update(@PathVariable Long id, @Valid @RequestBody DomainDefinitionDto dto) {
        return service.update(id, dto);
    }

    @PutMapping("/{id}/entities")
    @Operation(summary = "Set the full list of entities linked to a domain")
    @Loggable
    public ResponseEntity<Void> setEntities(@PathVariable Long id, @RequestBody List<Long> entityIds) {
        service.setEntities(id, entityIds);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/database-models")
    @Operation(summary = "Set the full list of database models linked to a domain")
    @Loggable
    public ResponseEntity<Void> setDatabaseModels(@PathVariable Long id, @RequestBody List<Long> dbModelIds) {
        service.setDatabaseModels(id, dbModelIds);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/generate-attributes")
    @Operation(summary = "Generate attributes from columns for all entities in a domain")
    @Loggable
    public GenerateAttributesResult generateAttributes(@PathVariable Long id) {
        return generateAttributesService.generateForDomain(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a domain definition")
    @Loggable
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
