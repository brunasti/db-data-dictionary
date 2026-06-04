package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.aspect.Loggable;
import it.brunasti.dbdadi.dto.AssociationDto;
import it.brunasti.dbdadi.service.AssociationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/associations")
@RequiredArgsConstructor
@Tag(name = "Associations", description = "Manage logical associations between entity definitions")
public class AssociationController {

    private final AssociationService service;

    @GetMapping
    @Operation(summary = "List all associations, optionally filtered by entity (from or to)")
    @Loggable
    public List<AssociationDto> findAll(@RequestParam(required = false) Long entityId) {
        if (entityId != null) return service.findByEntity(entityId);
        return service.findAll();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an association by ID")
    @Loggable
    public AssociationDto findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new association")
    @Loggable
    public AssociationDto create(@Valid @RequestBody AssociationDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update an association")
    @Loggable
    public AssociationDto update(@PathVariable Long id, @Valid @RequestBody AssociationDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete an association")
    @Loggable
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
