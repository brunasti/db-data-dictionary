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

        // Group unlinked tables by singularised name; keep groups spanning 2+ distinct DB models
        Map<String, List<TableDefinition>> byName = allTables.stream()
                .filter(t -> t.getEntity() == null)
                .collect(Collectors.groupingBy(t -> singularize(t.getName())));

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

        // Entity-scoped: find matching unlinked columns within each entity suggestion's tables
        List<AnalysisAttributeSuggestion> attributeSuggestions = new ArrayList<>();

        for (AnalysisEntitySuggestion es : entitySuggestions) {
            List<ColumnDefinition> scopedColumns = es.getTableIds().stream()
                    .flatMap(tid -> columnRepository.findByTableIdOrderByOrdinalPosition(tid).stream())
                    .filter(c -> c.getAttribute() == null)
                    .toList();

            Map<String, List<ColumnDefinition>> colsByName = scopedColumns.stream()
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

        // Global sweep: columns matching across 2+ DB models, not already covered above
        Set<Long> coveredColumnIds = attributeSuggestions.stream()
                .flatMap(s -> s.getColumnIds().stream())
                .collect(Collectors.toCollection(HashSet::new));

        List<ColumnDefinition> allUnlinkedColumns = allTables.stream()
                .flatMap(t -> columnRepository.findByTableIdOrderByOrdinalPosition(t.getId()).stream())
                .filter(c -> c.getAttribute() == null)
                .toList();

        Map<String, List<ColumnDefinition>> globalColsByName = allUnlinkedColumns.stream()
                .filter(c -> !coveredColumnIds.contains(c.getId()))
                .collect(Collectors.groupingBy(c -> c.getName().toUpperCase(Locale.ROOT).trim()));

        globalColsByName.entrySet().stream()
                .filter(e -> {
                    long distinctDbs = e.getValue().stream()
                            .map(c -> c.getTable().getSchema().getDatabaseModel().getId())
                            .distinct().count();
                    return distinctDbs >= 2;
                })
                .sorted(Map.Entry.comparingByKey())
                .map(e -> buildAttributeSuggestion("", e.getKey(), e.getValue()))
                .forEach(attributeSuggestions::add);

        return AnalysisResult.builder()
                .entitySuggestions(entitySuggestions)
                .attributeSuggestions(attributeSuggestions)
                .tablesAnalyzed(allTables.size())
                .columnsAnalyzed(allUnlinkedColumns.size())
                .build();
    }

    @Transactional
    public AnalysisApplyResult apply(AnalysisApplyRequest request) {
        int entitiesCreated = 0, entitiesReused = 0, tablesLinked = 0;
        int attributesCreated = 0, attributesReused = 0, columnsLinked = 0;

        Map<String, EntityDefinition> entityByNormName = new HashMap<>();

        // Resolve (or create) the target domain once for all entity suggestions
        DomainDefinition targetDomain = null;
        if (request.getDomainName() != null && !request.getDomainName().isBlank()) {
            targetDomain = domainRepository.findByNameIgnoreCase(request.getDomainName().trim())
                    .orElseGet(() -> domainRepository.save(
                            DomainDefinition.builder().name(request.getDomainName().trim()).build()));
        }
        final DomainDefinition domain = targetDomain;

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

            if (domain != null && !domain.getEntities().contains(entity)) {
                domain.getEntities().add(entity);
                domainRepository.save(domain);
            }
        }

        for (AnalysisAttributeSuggestion suggestion : orEmpty(request.getAttributes())) {
            boolean hasEntity = suggestion.getEntityName() != null && !suggestion.getEntityName().isBlank();

            EntityDefinition parentEntity = null;
            if (hasEntity) {
                parentEntity = entityByNormName.get(suggestion.getEntityName().toUpperCase(Locale.ROOT));
                if (parentEntity == null) {
                    parentEntity = entityRepository.findByNameIgnoreCase(suggestion.getEntityName()).orElse(null);
                }
                if (parentEntity == null) continue; // named entity required but not found
            }
            // parentEntity stays null for cross-entity suggestions — entity is optional in the model

            final EntityDefinition finalEntity = parentEntity;
            AttributeDefinition attribute;
            if (suggestion.getExistingAttributeId() != null) {
                attribute = attributeRepository.findById(suggestion.getExistingAttributeId())
                        .orElseThrow(() -> new it.brunasti.dbdadi.exception.ResourceNotFoundException(
                                "AttributeDefinition", suggestion.getExistingAttributeId()));
                attributesReused++;
            } else {
                Optional<AttributeDefinition> existing = finalEntity != null
                        ? attributeRepository.findByNameIgnoreCaseAndEntityId(suggestion.getSuggestedName(), finalEntity.getId())
                        : attributeRepository.findByNameIgnoreCase(suggestion.getSuggestedName());
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

        // Prefer the table whose name is already singular (i.e. already equal to the normalised key)
        String displayName = tables.stream()
                .filter(t -> singularize(t.getName()).equals(normName))
                .map(TableDefinition::getName)
                .findFirst()
                .orElse(normName); // fall back to the singularised key itself

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

        Long existingId;
        if (entityName == null || entityName.isBlank()) {
            existingId = attributeRepository.findByNameIgnoreCase(normName)
                    .map(AttributeDefinition::getId).orElse(null);
        } else {
            existingId = entityRepository.findByNameIgnoreCase(entityName)
                    .flatMap(e -> attributeRepository.findByNameIgnoreCaseAndEntityId(normName, e.getId()))
                    .map(AttributeDefinition::getId).orElse(null);
        }

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

    /**
     * Reduces an English noun to its singular upper-case form so that
     * CUSTOMER / CUSTOMERS, CATEGORY / CATEGORIES, ADDRESS / ADDRESSES, etc.
     * all map to the same grouping key.
     *
     * Rules (applied in order):
     *  IES  → Y          (CATEGORIES → CATEGORY, ENTITIES → ENTITY)
     *  SES  → S          (STATUSES → STATUS, ADDRESSES → ADDRESS)
     *  XES  → X          (BOXES → BOX)
     *  ZES  → Z          (BUZZES → BUZZ)
     *  CHES → CH         (CHURCHES → CHURCH)
     *  SHES → SH         (DISHES → DISH)
     *  SS / US / IS → unchanged  (STATUS, ADDRESS, BASIS — already singular)
     *  S    → remove S   (CUSTOMERS → CUSTOMER, TYPES → TYPE)
     */
    static String singularize(String word) {
        String w = word.toUpperCase(Locale.ROOT).trim();
        if (w.length() <= 1) return w;

        if (w.endsWith("IES"))  return w.substring(0, w.length() - 3) + "Y";
        if (w.endsWith("SES"))  return w.substring(0, w.length() - 2);
        if (w.endsWith("XES"))  return w.substring(0, w.length() - 2);
        if (w.endsWith("ZES"))  return w.substring(0, w.length() - 2);
        if (w.endsWith("CHES")) return w.substring(0, w.length() - 2);
        if (w.endsWith("SHES")) return w.substring(0, w.length() - 2);
        if (w.endsWith("SS") || w.endsWith("US") || w.endsWith("IS")) return w;
        if (w.endsWith("S"))    return w.substring(0, w.length() - 1);

        return w;
    }
}
