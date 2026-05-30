package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.*;
import it.brunasti.dbdadi.model.*;
import it.brunasti.dbdadi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalysisService {

    private final TableDefinitionRepository tableRepository;
    private final ColumnDefinitionRepository columnRepository;
    private final EntityDefinitionRepository entityRepository;
    private final AttributeDefinitionRepository attributeRepository;
    private final DomainDefinitionRepository domainRepository;

    public AnalysisResult analyze() {
        List<TableDefinition> allTables = tableRepository.findAll();

        // Group unlinked tables by normalised name; keep groups spanning 2+ distinct DB models
        Map<String, List<TableDefinition>> byName = allTables.stream()
                .filter(t -> t.getEntity() == null)
                .collect(Collectors.groupingBy(t -> t.getName().toUpperCase(Locale.ROOT).trim()));

        List<AnalysisEntitySuggestion> entitySuggestions = byName.entrySet().stream()
                .filter(e -> {
                    long distinctDbs = e.getValue().stream()
                            .map(t -> t.getSchema().getDatabaseModel().getId())
                            .distinct().count();
                    return distinctDbs >= 2;
                })
                .sorted(Map.Entry.comparingByKey())
                .map(e -> buildEntitySuggestion(e.getKey(), e.getValue()))
                .toList();

        // For each entity suggestion, find matching unlinked columns across those tables
        List<AnalysisAttributeSuggestion> attributeSuggestions = new ArrayList<>();
        int totalColumns = 0;

        for (AnalysisEntitySuggestion es : entitySuggestions) {
            List<ColumnDefinition> allColumns = es.getTableIds().stream()
                    .flatMap(tid -> columnRepository.findByTableIdOrderByOrdinalPosition(tid).stream())
                    .filter(c -> c.getAttribute() == null)
                    .toList();
            totalColumns += allColumns.size();

            Map<String, List<ColumnDefinition>> colsByName = allColumns.stream()
                    .collect(Collectors.groupingBy(c -> c.getName().toUpperCase(Locale.ROOT).trim()));

            colsByName.entrySet().stream()
                    .filter(e -> {
                        long distinctTables = e.getValue().stream()
                                .map(c -> c.getTable().getId())
                                .distinct().count();
                        return distinctTables >= 2;
                    })
                    .sorted(Map.Entry.comparingByKey())
                    .map(e -> buildAttributeSuggestion(es.getSuggestedName(), e.getKey(), e.getValue()))
                    .forEach(attributeSuggestions::add);
        }

        return AnalysisResult.builder()
                .entitySuggestions(entitySuggestions)
                .attributeSuggestions(attributeSuggestions)
                .tablesAnalyzed(allTables.size())
                .columnsAnalyzed(totalColumns)
                .build();
    }

    @Transactional
    public AnalysisApplyResult apply(AnalysisApplyRequest request) {
        int entitiesCreated = 0, entitiesReused = 0, tablesLinked = 0;
        int attributesCreated = 0, attributesReused = 0, columnsLinked = 0;

        Map<String, EntityDefinition> entityByNormName = new HashMap<>();

        for (AnalysisEntitySuggestion suggestion : orEmpty(request.getEntities())) {
            EntityDefinition entity;
            if (suggestion.getExistingEntityId() != null) {
                entity = entityRepository.findById(suggestion.getExistingEntityId())
                        .orElseThrow(() -> new it.brunasti.dbdadi.exception.ResourceNotFoundException(
                                "EntityDefinition", suggestion.getExistingEntityId()));
                entitiesReused++;
            } else {
                Optional<EntityDefinition> existing =
                        entityRepository.findByNameIgnoreCase(suggestion.getSuggestedName());
                if (existing.isPresent()) {
                    entity = existing.get();
                    entitiesReused++;
                } else {
                    entity = entityRepository.save(EntityDefinition.builder()
                            .name(suggestion.getSuggestedName())
                            .build());
                    entitiesCreated++;
                }
            }
            entityByNormName.put(suggestion.getSuggestedName().toUpperCase(Locale.ROOT), entity);

            for (Long tableId : orEmpty(suggestion.getTableIds())) {
                TableDefinition table = tableRepository.findById(tableId).orElse(null);
                if (table != null && table.getEntity() == null) {
                    table.setEntity(entity);
                    tableRepository.save(table);
                    tablesLinked++;
                }
            }

            if (request.getDomainId() != null) {
                DomainDefinition domain = domainRepository.findById(request.getDomainId()).orElse(null);
                if (domain != null && !domain.getEntities().contains(entity)) {
                    domain.getEntities().add(entity);
                    domainRepository.save(domain);
                }
            }
        }

        for (AnalysisAttributeSuggestion suggestion : orEmpty(request.getAttributes())) {
            EntityDefinition parentEntity = entityByNormName
                    .get(suggestion.getEntityName().toUpperCase(Locale.ROOT));
            if (parentEntity == null) {
                parentEntity = entityRepository.findByNameIgnoreCase(suggestion.getEntityName()).orElse(null);
            }
            if (parentEntity == null) continue;

            final EntityDefinition finalEntity = parentEntity;
            AttributeDefinition attribute;
            if (suggestion.getExistingAttributeId() != null) {
                attribute = attributeRepository.findById(suggestion.getExistingAttributeId())
                        .orElseThrow(() -> new it.brunasti.dbdadi.exception.ResourceNotFoundException(
                                "AttributeDefinition", suggestion.getExistingAttributeId()));
                attributesReused++;
            } else {
                Optional<AttributeDefinition> existing = attributeRepository
                        .findByNameIgnoreCaseAndEntityId(suggestion.getSuggestedName(), finalEntity.getId());
                if (existing.isPresent()) {
                    attribute = existing.get();
                    attributesReused++;
                } else {
                    attribute = attributeRepository.save(AttributeDefinition.builder()
                            .name(suggestion.getSuggestedName())
                            .entity(finalEntity)
                            .build());
                    attributesCreated++;
                }
            }

            for (Long columnId : orEmpty(suggestion.getColumnIds())) {
                ColumnDefinition column = columnRepository.findById(columnId).orElse(null);
                if (column != null && column.getAttribute() == null) {
                    column.setAttribute(attribute);
                    columnRepository.save(column);
                    columnsLinked++;
                }
            }
        }

        return AnalysisApplyResult.builder()
                .entitiesCreated(entitiesCreated)
                .entitiesReused(entitiesReused)
                .tablesLinked(tablesLinked)
                .attributesCreated(attributesCreated)
                .attributesReused(attributesReused)
                .columnsLinked(columnsLinked)
                .build();
    }

    // -------------------------------------------------------------------------

    private AnalysisEntitySuggestion buildEntitySuggestion(String normName, List<TableDefinition> tables) {
        Long existingId = entityRepository.findByNameIgnoreCase(normName)
                .map(EntityDefinition::getId).orElse(null);

        // Use the original casing from the first table
        String displayName = tables.get(0).getName();

        return AnalysisEntitySuggestion.builder()
                .suggestedName(displayName)
                .existingEntityId(existingId)
                .tableIds(tables.stream().map(TableDefinition::getId).toList())
                .tableLabels(tables.stream().map(t ->
                        t.getSchema().getDatabaseModel().getName()
                        + " / " + t.getSchema().getName()
                        + " / " + t.getName()).toList())
                .build();
    }

    private AnalysisAttributeSuggestion buildAttributeSuggestion(
            String entityName, String normName, List<ColumnDefinition> columns) {

        String displayName = columns.get(0).getName();

        Long existingId = entityRepository.findByNameIgnoreCase(entityName)
                .flatMap(e -> attributeRepository.findByNameIgnoreCaseAndEntityId(normName, e.getId()))
                .map(AttributeDefinition::getId).orElse(null);

        return AnalysisAttributeSuggestion.builder()
                .suggestedName(displayName)
                .entityName(entityName)
                .existingAttributeId(existingId)
                .columnIds(columns.stream().map(ColumnDefinition::getId).toList())
                .columnLabels(columns.stream().map(c ->
                        c.getTable().getSchema().getDatabaseModel().getName()
                        + " / " + c.getTable().getSchema().getName()
                        + " / " + c.getTable().getName()
                        + " / " + c.getName()).toList())
                .build();
    }

    private <T> List<T> orEmpty(List<T> list) {
        return list == null ? List.of() : list;
    }
}
