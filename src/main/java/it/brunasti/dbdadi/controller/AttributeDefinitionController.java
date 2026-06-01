package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.aspect.Loggable;
import it.brunasti.dbdadi.dto.AttributeDefinitionDto;
import it.brunasti.dbdadi.dto.AttributeEntitySuggestion;
import it.brunasti.dbdadi.dto.MergeAttributeRequest;
import it.brunasti.dbdadi.dto.MergeAttributeResult;
import it.brunasti.dbdadi.service.AttributeDefinitionService;
import it.brunasti.dbdadi.service.MergeAttributeService;
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
    private final MergeAttributeService mergeService;

    @GetMapping
    @Operation(summary = "List all attribute definitions")
    @Loggable
    public List<AttributeDefinitionDto> findAll(
            @RequestParam(required = false) Long entityId) {
        if (entityId != null) return service.findByEntity(entityId);
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an attribute definition by ID")
    @Loggable
    public AttributeDefinitionDto findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new attribute definition")
    @Loggable
    public AttributeDefinitionDto create(@Valid @RequestBody AttributeDefinitionDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an attribute definition")
    @Loggable
    public AttributeDefinitionDto update(@PathVariable Long id, @Valid @RequestBody AttributeDefinitionDto dto) {
        return service.update(id, dto);
    }

    @PostMapping("/merge")
    @Operation(summary = "Merge two attributes: move all column links from source to target, then delete source")
    @Loggable
    public MergeAttributeResult merge(@RequestBody MergeAttributeRequest request) {
        return mergeService.merge(request);
    }

    @GetMapping("/{id}/suggested-entities")
    @Operation(summary = "Suggest entities to link to this attribute, based on the tables of its linked columns")
    @Loggable
    public List<AttributeEntitySuggestion> suggestEntities(@PathVariable Long id) {
        return service.suggestEntities(id);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an attribute definition")
    @Loggable
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
