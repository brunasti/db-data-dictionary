package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/reset")
@RequiredArgsConstructor
@Tag(name = "Admin - Reset", description = "Reset (delete all) data from selected groups of tables")
public class ResetController {

    private final RelationshipDefinitionRepository relationshipRepo;
    private final ColumnDefinitionRepository columnRepo;
    private final TableDefinitionRepository tableRepo;
    private final SchemaDefinitionRepository schemaRepo;
    private final DatabaseModelRepository dbModelRepo;
    private final AttributeDefinitionRepository attributeRepo;
    private final EntityDefinitionRepository entityRepo;

    @DeleteMapping("/database")
    @Operation(summary = "Delete all Database Models, Schemas, Tables, Columns and Relationships")
    public ResponseEntity<Void> resetDatabase() {
        relationshipRepo.deleteAll();
        columnRepo.deleteAll();
        tableRepo.deleteAll();
        schemaRepo.deleteAll();
        dbModelRepo.deleteAll();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/modeling")
    @Operation(summary = "Delete all Entities and Attributes")
    public ResponseEntity<Void> resetModeling() {
        attributeRepo.deleteAll();
        entityRepo.deleteAll();
        return ResponseEntity.noContent().build();
    }
}
