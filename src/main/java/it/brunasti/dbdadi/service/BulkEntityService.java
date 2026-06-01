package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.BulkEntityRequest;
import it.brunasti.dbdadi.dto.BulkEntityResult;
import it.brunasti.dbdadi.exception.ResourceNotFoundException;
import it.brunasti.dbdadi.model.DomainDefinition;
import it.brunasti.dbdadi.model.EntityDefinition;
import it.brunasti.dbdadi.model.TableDefinition;
import it.brunasti.dbdadi.repository.DomainDefinitionRepository;
import it.brunasti.dbdadi.repository.EntityDefinitionRepository;
import it.brunasti.dbdadi.repository.TableDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BulkEntityService {

    private final DomainDefinitionRepository domainRepository;
    private final EntityDefinitionRepository entityRepository;
    private final TableDefinitionRepository tableRepository;

    @Transactional
    public BulkEntityResult createEntitiesForUnmatchedTables(BulkEntityRequest request) {
        if (request.getDomainId() == null) {
            throw new IllegalArgumentException("domainId is required");
        }
        if (request.getDatabaseModelIds() == null || request.getDatabaseModelIds().isEmpty()) {
            throw new IllegalArgumentException("At least one databaseModelId is required");
        }

        DomainDefinition domain = domainRepository.findById(request.getDomainId())
                .orElseThrow(() -> new ResourceNotFoundException("DomainDefinition", request.getDomainId()));

        int entitiesCreated = 0, entitiesReused = 0, tablesLinked = 0, tablesSkipped = 0;
        List<String> createdNames = new java.util.ArrayList<>();
        List<String> warnings = new java.util.ArrayList<>();

        for (Long dbModelId : request.getDatabaseModelIds()) {
            List<TableDefinition> tables = tableRepository.findBySchema_DatabaseModel_Id(dbModelId);

            for (TableDefinition table : tables) {
                if (table.getEntity() != null) {
                    tablesSkipped++;
                    continue;
                }

                String entityName = toEntityName(table.getName());

                EntityDefinition entity = entityRepository.findByNameIgnoreCase(entityName).orElse(null);

                if (entity == null) {
                    entity = EntityDefinition.builder()
                            .name(entityName)
                            .description("Auto-created from table: " + table.getName())
                            .build();
                    entity = entityRepository.save(entity);
                    entitiesCreated++;
                    createdNames.add(entityName);
                    log.info("Created entity '{}' from table '{}'", entityName, table.getName());
                } else {
                    entitiesReused++;
                    log.info("Reused existing entity '{}' for table '{}'", entityName, table.getName());
                }

                // Link table → entity
                table.setEntity(entity);
                tableRepository.save(table);
                tablesLinked++;

                // Add entity to domain if not already linked
                EntityDefinition finalEntity = entity;
                boolean alreadyInDomain = domain.getEntities().stream()
                        .anyMatch(e -> e.getId().equals(finalEntity.getId()));
                if (!alreadyInDomain) {
                    domain.getEntities().add(entity);
                }
            }
        }

        domainRepository.save(domain);

        return BulkEntityResult.builder()
                .entitiesCreated(entitiesCreated)
                .entitiesReused(entitiesReused)
                .tablesLinked(tablesLinked)
                .tablesSkipped(tablesSkipped)
                .createdNames(createdNames)
                .warnings(warnings)
                .build();
    }

    // Convert snake_case / UPPER_CASE table names to PascalCase entity names
    private String toEntityName(String tableName) {
        if (tableName == null || tableName.isBlank()) return tableName;
        return Arrays.stream(tableName.split("[_\\s]+"))
                .filter(s -> !s.isBlank())
                .map(s -> Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase())
                .collect(Collectors.joining());
    }
}
