package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.dto.AttributeDefinitionDto;
import it.brunasti.dbdadi.service.AttributeDefinitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/attributes")
@RequiredArgsConstructor
@Tag(name = "Attribute Definitions", description = "Manage logical attribute definitions")
public class AttributeDefinitionController {

    private final AttributeDefinitionService service;

    @GetMapping
    @Operation(summary = "List all attribute definitions")
    public List<AttributeDefinitionDto> findAll(
            @RequestParam(required = false) Long entityId) {
        if (entityId != null) return service.findByEntity(entityId);
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an attribute definition by ID")
    public AttributeDefinitionDto findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new attribute definition")
    public AttributeDefinitionDto create(@Valid @RequestBody AttributeDefinitionDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an attribute definition")
    public AttributeDefinitionDto update(@PathVariable Long id, @Valid @RequestBody AttributeDefinitionDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an attribute definition")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
