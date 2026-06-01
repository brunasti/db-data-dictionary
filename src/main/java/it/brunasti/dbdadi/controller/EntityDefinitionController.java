package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.aspect.Loggable;
import it.brunasti.dbdadi.dto.BulkEntityRequest;
import it.brunasti.dbdadi.dto.BulkEntityResult;
import it.brunasti.dbdadi.dto.DomainDefinitionDto;
import it.brunasti.dbdadi.dto.EntityDefinitionDto;
import it.brunasti.dbdadi.service.BulkEntityService;
import it.brunasti.dbdadi.service.EntityDefinitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/entities")
@RequiredArgsConstructor
@Tag(name = "Entity Definitions", description = "Manage logical entity definitions")
public class EntityDefinitionController {

    private final EntityDefinitionService service;
    private final BulkEntityService bulkEntityService;

    @GetMapping
    @Operation(summary = "List all entity definitions, optionally filtered by domain")
    @Loggable
    public List<EntityDefinitionDto> findAll(@RequestParam(required = false) Long domainId) {
        if (domainId != null) {
            return service.findByDomain(domainId);
        }
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an entity definition by ID")
    @Loggable
    public EntityDefinitionDto findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new entity definition")
    @Loggable
    public EntityDefinitionDto create(@Valid @RequestBody EntityDefinitionDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an entity definition")
    @Loggable
    public EntityDefinitionDto update(@PathVariable Long id, @Valid @RequestBody EntityDefinitionDto dto) {
        return service.update(id, dto);
    }

    @GetMapping("/{id}/domains")
    @Operation(summary = "Get all domains this entity belongs to")
    @Loggable
    public List<DomainDefinitionDto> findDomains(@PathVariable Long id) {
        return service.findDomains(id);
    }

    @PutMapping("/{id}/domains")
    @Operation(summary = "Set the full list of domains this entity belongs to")
    @Loggable
    public ResponseEntity<Void> setDomains(@PathVariable Long id, @RequestBody List<Long> domainIds) {
        service.setDomains(id, domainIds);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bulk-create")
    @Operation(summary = "Create entities for all unmatched tables in selected database models, linked to a domain")
    @Loggable
    public BulkEntityResult bulkCreate(@RequestBody BulkEntityRequest request) {
        return bulkEntityService.createEntitiesForUnmatchedTables(request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an entity definition")
    @Loggable
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
