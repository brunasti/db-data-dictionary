package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.GenerateAttributesResult;
import it.brunasti.dbdadi.exception.ResourceNotFoundException;
import it.brunasti.dbdadi.model.AttributeDefinition;
import it.brunasti.dbdadi.model.ColumnDefinition;
import it.brunasti.dbdadi.model.EntityDefinition;
import it.brunasti.dbdadi.model.TableDefinition;
import it.brunasti.dbdadi.repository.AttributeDefinitionRepository;
import it.brunasti.dbdadi.repository.ColumnDefinitionRepository;
import it.brunasti.dbdadi.repository.EntityDefinitionRepository;
import it.brunasti.dbdadi.repository.TableDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GenerateAttributesService {

    private final EntityDefinitionRepository entityRepository;
    private final TableDefinitionRepository tableRepository;
    private final ColumnDefinitionRepository columnRepository;
    private final AttributeDefinitionRepository attributeRepository;

    @Transactional
    public GenerateAttributesResult generateForEntity(Long entityId) {
        EntityDefinition entity = entityRepository.findById(entityId)
                .orElseThrow(() -> new ResourceNotFoundException("EntityDefinition", entityId));

        List<TableDefinition> tables = tableRepository.findByEntityId(entityId);

        // Pass 1: collect all unlinked columns, grouped by lowercase name so that
        // same-named columns across different tables map to a single attribute.
        Map<String, List<ColumnDefinition>> columnsByName = new LinkedHashMap<>();
        int columnsAlreadyLinked = 0;

        for (TableDefinition table : tables) {
            for (ColumnDefinition col :
                    columnRepository.findByTableIdOrderByOrdinalPosition(table.getId())) {
                if (col.getAttribute() != null) {
                    columnsAlreadyLinked++;
                    continue;
                }
                columnsByName
                        .computeIfAbsent(col.getName().toLowerCase(), k -> new ArrayList<>())
                        .add(col);
            }
        }

        // Pass 2: for each unique column name, find or create one attribute and link
        // every column in that group to it.
        List<String> createdNames = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int columnsLinked = 0;

        for (List<ColumnDefinition> group : columnsByName.values()) {
            String canonicalName = group.get(0).getName(); // preserve original casing

            Optional<AttributeDefinition> existing =
                    attributeRepository.findByNameIgnoreCase(canonicalName);

            AttributeDefinition attr;
            if (existing.isPresent()) {
                attr = existing.get();
                if (attr.getEntity() == null) {
                    attr.setEntity(entity);
                    attributeRepository.save(attr);
                } else if (!attr.getEntity().getId().equals(entityId)) {
                    warnings.add("Attribute '" + canonicalName + "' already belongs to entity '"
                            + attr.getEntity().getName() + "' — columns linked to it anyway.");
                }
            } else {
                // Use the description from the first column that has one
                String description = group.stream()
                        .map(ColumnDefinition::getDescription)
                        .filter(d -> d != null && !d.isBlank())
                        .findFirst()
                        .orElse(null);
                attr = AttributeDefinition.builder()
                        .name(canonicalName)
                        .description(description)
                        .entity(entity)
                        .build();
                attributeRepository.save(attr);
                createdNames.add(canonicalName);
                log.info("Created attribute '{}' covering {} column(s)", canonicalName, group.size());
            }

            for (ColumnDefinition col : group) {
                col.setAttribute(attr);
                columnRepository.save(col);
                columnsLinked++;
                log.info("Linked column '{}' (table '{}') → attribute '{}'",
                        col.getName(),
                        col.getTable() != null ? col.getTable().getName() : "?",
                        attr.getName());
            }
        }

        return GenerateAttributesResult.builder()
                .attributesCreated(createdNames.size())
                .columnsLinked(columnsLinked)
                .columnsAlreadyLinked(columnsAlreadyLinked)
                .createdNames(createdNames)
                .warnings(warnings)
                .build();
    }
}
