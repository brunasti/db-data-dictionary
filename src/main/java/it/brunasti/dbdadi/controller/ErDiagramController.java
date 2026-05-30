package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.service.ErDiagramService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/er-diagram")
@RequiredArgsConstructor
@Tag(name = "ER Diagram", description = "Generates PlantUML entity-relationship diagrams from the logical model")
public class ErDiagramController {

    private final ErDiagramService service;

    @GetMapping(produces = "text/plain;charset=UTF-8")
    @Operation(summary = "Generate a PlantUML ER diagram; optionally filtered to a single domain")
    public String generate(@RequestParam(required = false) Long domainId) {
        return service.generate(domainId);
    }

    @GetMapping(value = "/schema/{schemaId}", produces = "text/plain;charset=UTF-8")
    @Operation(summary = "Generate a PlantUML physical ER diagram for a schema (tables, columns, relationships)")
    public String generateForSchema(@PathVariable Long schemaId) {
        return service.generateForSchema(schemaId);
    }
}
