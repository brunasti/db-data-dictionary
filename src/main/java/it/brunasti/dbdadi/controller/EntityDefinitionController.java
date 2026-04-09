package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.dto.EntityDefinitionDto;
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

    @GetMapping
    @Operation(summary = "List all entity definitions")
    public List<EntityDefinitionDto> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an entity definition by ID")
    public EntityDefinitionDto findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new entity definition")
    public EntityDefinitionDto create(@Valid @RequestBody EntityDefinitionDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an entity definition")
    public EntityDefinitionDto update(@PathVariable Long id, @Valid @RequestBody EntityDefinitionDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an entity definition")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
