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
import java.util.List;
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

        List<String> createdNames = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int columnsLinked = 0;
        int columnsAlreadyLinked = 0;

        for (TableDefinition table : tables) {
            List<ColumnDefinition> columns =
                    columnRepository.findByTableIdOrderByOrdinalPosition(table.getId());

            for (ColumnDefinition col : columns) {
                if (col.getAttribute() != null) {
                    columnsAlreadyLinked++;
                    continue;
                }

                Optional<AttributeDefinition> existing =
                        attributeRepository.findByNameIgnoreCase(col.getName());

                AttributeDefinition attr;
                if (existing.isPresent()) {
                    attr = existing.get();
                    if (attr.getEntity() == null) {
                        attr.setEntity(entity);
                        attributeRepository.save(attr);
                    } else if (!attr.getEntity().getId().equals(entityId)) {
                        warnings.add("Column '" + table.getName() + "." + col.getName()
                                + "': attribute already linked to entity '"
                                + attr.getEntity().getName() + "' — column linked to it anyway.");
                    }
                    log.info("Linked column '{}.{}' to existing attribute '{}'",
                            table.getName(), col.getName(), attr.getName());
                } else {
                    attr = AttributeDefinition.builder()
                            .name(col.getName())
                            .description(col.getDescription())
                            .entity(entity)
                            .build();
                    attributeRepository.save(attr);
                    createdNames.add(attr.getName());
                    log.info("Created attribute '{}' for column '{}.{}'",
                            attr.getName(), table.getName(), col.getName());
                }

                col.setAttribute(attr);
                columnRepository.save(col);
                columnsLinked++;
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
