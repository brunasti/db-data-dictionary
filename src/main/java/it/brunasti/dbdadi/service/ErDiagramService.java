package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.exception.ResourceNotFoundException;
import it.brunasti.dbdadi.model.*;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import it.brunasti.dbdadi.model.enums.RelationshipType;
import it.brunasti.dbdadi.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ErDiagramService {

    private final DomainDefinitionRepository domainRepository;
    private final EntityDefinitionRepository entityRepository;
    private final AttributeDefinitionRepository attributeRepository;
    private final RelationshipDefinitionRepository relationshipRepository;
    private final TableDefinitionRepository tableRepository;
    private final ColumnDefinitionRepository columnRepository;

    // -------------------------------------------------------------------------
    // Logical ER diagram: Domains → Entities → Attributes
    // -------------------------------------------------------------------------

    public String generate(Long domainId) {
        List<DomainDefinition> domains;
        List<EntityDefinition> entities;

        if (domainId != null) {
            DomainDefinition domain = domainRepository.findById(domainId)
                    .orElseThrow(() -> new ResourceNotFoundException("DomainDefinition", domainId));
            domains = List.of(domain);
            entities = new ArrayList<>(domain.getEntities());
        } else {
            domains = domainRepository.findAllByOrderByNameAsc();
            entities = entityRepository.findAll();
        }

        Set<Long> entityIds = entities.stream()
                .map(EntityDefinition::getId)
                .collect(Collectors.toSet());
        Map<Long, EntityDefinition> entityById = entities.stream()
                .collect(Collectors.toMap(EntityDefinition::getId, e -> e));

        // Map table-level relationships to entity-level pairs, deduplicating by (fromEntity, toEntity)
        record RelKey(Long from, Long to) {}
        Map<RelKey, List<RelationshipDefinition>> relsByPair = new LinkedHashMap<>();
        for (RelationshipDefinition rel : relationshipRepository.findAll()) {
            EntityDefinition fromEntity = rel.getFromTable().getEntity();
            EntityDefinition toEntity = rel.getToTable().getEntity();
            if (fromEntity == null || toEntity == null) continue;
            if (fromEntity.getId().equals(toEntity.getId())) continue;
            if (!entityIds.contains(fromEntity.getId()) || !entityIds.contains(toEntity.getId())) continue;
            relsByPair.computeIfAbsent(new RelKey(fromEntity.getId(), toEntity.getId()), k -> new ArrayList<>())
                    .add(rel);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("skinparam packageStyle rectangle\n");
        sb.append("skinparam linetype ortho\n\n");

        // Emit entities grouped inside their domain package (each entity placed once, in its first domain)
        Set<Long> placed = new LinkedHashSet<>();
        for (DomainDefinition domain : domains) {
            List<EntityDefinition> domainEntities = domain.getEntities().stream()
                    .filter(e -> entityIds.contains(e.getId()))
                    .sorted(Comparator.comparing(EntityDefinition::getName))
                    .toList();
            if (domainEntities.isEmpty()) continue;
            sb.append("package \"").append(escape(domain.getName())).append("\" {\n");
            for (EntityDefinition e : domainEntities) {
                if (placed.add(e.getId())) {
                    appendLogicalEntity(sb, e, "  ");
                }
            }
            sb.append("}\n\n");
        }

        // Entities with no domain, or not yet placed
        entities.stream()
                .filter(e -> !placed.contains(e.getId()))
                .sorted(Comparator.comparing(EntityDefinition::getName))
                .forEach(e -> {
                    appendLogicalEntity(sb, e, "");
                    sb.append("\n");
                });

        // Emit relationships
        if (!relsByPair.isEmpty()) {
            sb.append("\n");
        }
        for (Map.Entry<RelKey, List<RelationshipDefinition>> entry : relsByPair.entrySet()) {
            RelKey key = entry.getKey();
            List<RelationshipDefinition> rels = entry.getValue();
            EntityDefinition fromEntity = entityById.get(key.from());
            EntityDefinition toEntity = entityById.get(key.to());
            RelationshipType type = rels.get(0).getType();
            String label = rels.stream()
                    .map(RelationshipDefinition::getName)
                    .distinct()
                    .collect(Collectors.joining(", "));
            sb.append(alias(fromEntity.getName()))
              .append(" ").append(arrow(type)).append(" ")
              .append(alias(toEntity.getName()))
              .append(" : \"").append(escape(label)).append("\"\n");
        }

        sb.append("@enduml\n");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Render PlantUML source to SVG
    // -------------------------------------------------------------------------

    public String generateSvg(Long domainId) {
        return renderToSvg(generate(domainId));
    }

    public String generateSvgForSchema(Long schemaId) {
        return renderToSvg(generateForSchema(schemaId));
    }

    private String renderToSvg(String plantuml) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            new SourceStringReader(plantuml).outputImage(out, new FileFormatOption(FileFormat.SVG));
            String raw = out.toString(StandardCharsets.UTF_8);
            int svgStart = raw.indexOf("<svg");
            return svgStart >= 0 ? raw.substring(svgStart) : raw;
        } catch (IOException e) {
            throw new RuntimeException("SVG generation failed", e);
        }
    }

    // -------------------------------------------------------------------------
    // Physical ER diagram: Schema → Tables → Columns + Relationships
    // -------------------------------------------------------------------------

    public String generateForSchema(Long schemaId) {
        List<TableDefinition> tables = tableRepository.findBySchemaId(schemaId);
        if (tables.isEmpty()) {
            return "@startuml\nnote \"No tables found in this schema\"\n@enduml\n";
        }

        Set<Long> schemaTableIds = tables.stream()
                .map(TableDefinition::getId)
                .collect(Collectors.toSet());

        // Columns per table
        Map<Long, List<ColumnDefinition>> columnsByTable = tables.stream()
                .collect(Collectors.toMap(
                        TableDefinition::getId,
                        t -> columnRepository.findByTableIdOrderByOrdinalPosition(t.getId())));

        // Relationships where at least one side is in this schema
        Set<Long> seenRelIds = new HashSet<>();
        List<RelationshipDefinition> allRels = new ArrayList<>();
        for (RelationshipDefinition r : relationshipRepository.findByFromTable_Schema_Id(schemaId)) {
            if (seenRelIds.add(r.getId())) allRels.add(r);
        }
        for (RelationshipDefinition r : relationshipRepository.findByToTable_Schema_Id(schemaId)) {
            if (seenRelIds.add(r.getId())) allRels.add(r);
        }

        // Collect external tables referenced by cross-schema relationships
        Map<Long, TableDefinition> externalTables = new LinkedHashMap<>();
        for (RelationshipDefinition r : allRels) {
            if (!schemaTableIds.contains(r.getFromTable().getId())) {
                externalTables.put(r.getFromTable().getId(), r.getFromTable());
            }
            if (!schemaTableIds.contains(r.getToTable().getId())) {
                externalTables.put(r.getToTable().getId(), r.getToTable());
            }
        }

        // Schema/DB header as a comment
        TableDefinition first = tables.get(0);
        String schemaName = first.getSchema().getName();
        String dbName = first.getSchema().getDatabaseModel().getName();

        StringBuilder sb = new StringBuilder();
        sb.append("@startuml\n");
        sb.append("' Schema: ").append(schemaName).append(" | Database: ").append(dbName).append("\n");
        sb.append("skinparam linetype ortho\n\n");

        // Internal tables with full column detail
        tables.stream()
                .sorted(Comparator.comparing(TableDefinition::getName))
                .forEach(t -> {
                    appendPhysicalTable(sb, t, columnsByTable.getOrDefault(t.getId(), List.of()), "");
                    sb.append("\n");
                });

        // External (cross-schema) tables as stub entities
        externalTables.values().stream()
                .sorted(Comparator.comparing(TableDefinition::getName))
                .forEach(t -> {
                    sb.append("entity \"").append(escape(t.getName()))
                      .append("\\n(").append(escape(t.getSchema().getName())).append(")")
                      .append("\" as ").append(externalAlias(t)).append(" {\n}\n\n");
                });

        // Relationships
        if (!allRels.isEmpty()) {
            sb.append("\n");
        }
        for (RelationshipDefinition rel : allRels) {
            String fromAlias = schemaTableIds.contains(rel.getFromTable().getId())
                    ? alias(rel.getFromTable().getName())
                    : externalAlias(rel.getFromTable());
            String toAlias = schemaTableIds.contains(rel.getToTable().getId())
                    ? alias(rel.getToTable().getName())
                    : externalAlias(rel.getToTable());
            sb.append(fromAlias).append(" ").append(arrow(rel.getType())).append(" ").append(toAlias)
              .append(" : \"").append(escape(rel.getName())).append("\"\n");
        }

        sb.append("@enduml\n");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void appendLogicalEntity(StringBuilder sb, EntityDefinition entity, String indent) {
        List<AttributeDefinition> attrs = attributeRepository.findByEntityIdOrderByNameAsc(entity.getId());
        sb.append(indent).append("entity \"").append(escape(entity.getName()))
          .append("\" as ").append(alias(entity.getName())).append(" {\n");
        for (AttributeDefinition attr : attrs) {
            sb.append(indent).append("  ").append(escape(attr.getName())).append("\n");
        }
        sb.append(indent).append("}\n");
    }

    private void appendPhysicalTable(StringBuilder sb, TableDefinition table,
                                     List<ColumnDefinition> cols, String indent) {
        sb.append(indent).append("entity \"").append(escape(table.getName()))
          .append("\" as ").append(alias(table.getName())).append(" {\n");

        List<ColumnDefinition> pkCols    = cols.stream().filter(ColumnDefinition::isPrimaryKey).toList();
        List<ColumnDefinition> otherCols = cols.stream().filter(c -> !c.isPrimaryKey()).toList();

        for (ColumnDefinition col : pkCols) {
            sb.append(indent).append("  * ").append(escape(col.getName()))
              .append(" : ").append(formatType(col)).append(" <<PK>>\n");
        }
        if (!pkCols.isEmpty() && !otherCols.isEmpty()) {
            sb.append(indent).append("  --\n");
        }
        for (ColumnDefinition col : otherCols) {
            String marker = col.isNullable() ? "  " : "  * ";
            sb.append(indent).append(marker).append(escape(col.getName()))
              .append(" : ").append(formatType(col)).append("\n");
        }
        sb.append(indent).append("}\n");
    }

    private static String formatType(ColumnDefinition col) {
        String type = col.getDataType() != null ? col.getDataType() : "?";
        if (col.getLength() != null && col.getLength() > 0) {
            type += "(" + col.getLength() + ")";
        } else if (col.getPrecision() != null && col.getPrecision() > 0) {
            String prec = col.getPrecision().toString();
            if (col.getScale() != null && col.getScale() > 0) prec += "," + col.getScale();
            type += "(" + prec + ")";
        }
        return type;
    }

    private static String arrow(RelationshipType type) {
        return switch (type) {
            case ONE_TO_ONE   -> "||--||";
            case ONE_TO_MANY  -> "||--o{";
            case MANY_TO_ONE  -> "}o--||";
            case MANY_TO_MANY -> "}o--o{";
        };
    }

    private static String alias(String name) {
        return name.replaceAll("[^A-Za-z0-9]", "_");
    }

    // External tables get the schema name appended to avoid alias collisions with internal tables
    private static String externalAlias(TableDefinition t) {
        return alias(t.getName()) + "_" + alias(t.getSchema().getName());
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("\"", "'");
    }
}
