package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.AttributeDefinitionDto;
import it.brunasti.dbdadi.dto.AttributeEntitySuggestion;
import it.brunasti.dbdadi.exception.ResourceNotFoundException;
import it.brunasti.dbdadi.model.AttributeDefinition;
import it.brunasti.dbdadi.model.ColumnDefinition;
import it.brunasti.dbdadi.model.EntityDefinition;
import it.brunasti.dbdadi.model.TableDefinition;
import it.brunasti.dbdadi.repository.AttributeDefinitionRepository;
import it.brunasti.dbdadi.repository.ColumnDefinitionRepository;
import it.brunasti.dbdadi.repository.EntityDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttributeDefinitionService {

    private final AttributeDefinitionRepository repository;
    private final EntityDefinitionRepository entityRepository;
    private final ColumnDefinitionRepository columnRepository;

    public List<AttributeDefinitionDto> findAll() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    public List<AttributeDefinitionDto> findByEntity(Long entityId) {
        return repository.findByEntityIdOrderByNameAsc(entityId).stream().map(this::toDto).toList();
    }

    public AttributeDefinitionDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    @Transactional
    public AttributeDefinitionDto create(AttributeDefinitionDto dto) {
        AttributeDefinition entity = AttributeDefinition.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .entity(resolveEntity(dto.getEntityId()))
                .build();
        return toDto(repository.save(entity));
    }

    @Transactional
    public AttributeDefinitionDto update(Long id, AttributeDefinitionDto dto) {
        AttributeDefinition existing = getOrThrow(id);
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setEntity(resolveEntity(dto.getEntityId()));
        return toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        getOrThrow(id);
        repository.deleteById(id);
    }

    /** Walk: attribute → columns → tables → entities, grouping tables by entity. */
    public List<AttributeEntitySuggestion> suggestEntities(Long attributeId) {
        getOrThrow(attributeId); // validate existence

        List<ColumnDefinition> columns = columnRepository.findByAttributeId(attributeId);

        // entity id → (entity, list of table names, column count)
        Map<Long, EntityDefinition> entityMap = new LinkedHashMap<>();
        Map<Long, List<String>> tableNamesByEntity = new LinkedHashMap<>();
        Map<Long, Integer> columnCountByEntity = new LinkedHashMap<>();

        for (ColumnDefinition col : columns) {
            TableDefinition table = col.getTable();
            if (table == null || table.getEntity() == null) continue;
            EntityDefinition entity = table.getEntity();
            entityMap.put(entity.getId(), entity);
            tableNamesByEntity.computeIfAbsent(entity.getId(), k -> new ArrayList<>())
                    .add(table.getName());
            columnCountByEntity.merge(entity.getId(), 1, Integer::sum);
        }

        return entityMap.values().stream()
                .sorted(Comparator.comparing(EntityDefinition::getName))
                .map(e -> AttributeEntitySuggestion.builder()
                        .entityId(e.getId())
                        .entityName(e.getName())
                        .entityDescription(e.getDescription())
                        .viaTableNames(tableNamesByEntity.getOrDefault(e.getId(), List.of()))
                        .linkedColumnsCount(columnCountByEntity.getOrDefault(e.getId(), 0))
                        .build())
                .toList();
    }

    private EntityDefinition resolveEntity(Long entityId) {
        if (entityId == null) return null;
        return entityRepository.findById(entityId)
                .orElseThrow(() -> new ResourceNotFoundException("EntityDefinition", entityId));
    }

    private AttributeDefinition getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttributeDefinition", id));
    }

    private AttributeDefinitionDto toDto(AttributeDefinition e) {
        return AttributeDefinitionDto.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .entityId(e.getEntity() != null ? e.getEntity().getId() : null)
                .entityName(e.getEntity() != null ? e.getEntity().getName() : null)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
