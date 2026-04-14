package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.ExcelImportResult;
import it.brunasti.dbdadi.model.*;
import it.brunasti.dbdadi.model.enums.DbType;
import it.brunasti.dbdadi.model.enums.RelationshipType;
import it.brunasti.dbdadi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelImportService {

    private final EntityDefinitionRepository entityRepo;
    private final AttributeDefinitionRepository attributeRepo;
    private final DatabaseModelRepository dbModelRepo;
    private final SchemaDefinitionRepository schemaRepo;
    private final TableDefinitionRepository tableRepo;
    private final ColumnDefinitionRepository columnRepo;
    private final RelationshipDefinitionRepository relationshipRepo;

    @Transactional
    public ExcelImportResult importFromExcel(byte[] data) throws IOException {
        List<String> warnings = new ArrayList<>();
        int skipped = 0;

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(data))) {
            ImportCounts c = new ImportCounts();
            skipped += importEntities(wb, c, warnings);
            skipped += importAttributes(wb, c, warnings);
            skipped += importDatabaseModels(wb, c, warnings);
            skipped += importSchemas(wb, c, warnings);
            skipped += importTables(wb, c, warnings);
            skipped += importColumns(wb, c, warnings);
            skipped += importRelationships(wb, c, warnings);

            return ExcelImportResult.builder()
                    .entitiesImported(c.entities)
                    .attributesImported(c.attributes)
                    .databaseModelsImported(c.dbModels)
                    .schemasImported(c.schemas)
                    .tablesImported(c.tables)
                    .columnsImported(c.columns)
                    .relationshipsImported(c.relationships)
                    .skipped(skipped)
                    .warnings(warnings)
                    .build();
        }
    }

    // ---- sheet importers ----

    private int importEntities(XSSFWorkbook wb, ImportCounts c, List<String> warnings) {
        Sheet sheet = wb.getSheet("Entities");
        if (sheet == null) return 0;
        Set<String> existing = entityRepo.findAll().stream()
                .map(EntityDefinition::getName).collect(Collectors.toSet());
        int skipped = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String name = str(row, 1);
            if (blank(name)) continue;
            if (existing.contains(name)) {
                warnings.add("Entity skipped (duplicate): " + name);
                skipped++;
                continue;
            }
            entityRepo.save(EntityDefinition.builder()
                    .name(name).description(str(row, 2)).build());
            existing.add(name);
            c.entities++;
        }
        return skipped;
    }

    private int importAttributes(XSSFWorkbook wb, ImportCounts c, List<String> warnings) {
        // col: 0=ID, 1=Name, 2=Description, 3=Entity ID, 4=Entity
        Sheet sheet = wb.getSheet("Attributes");
        if (sheet == null) return 0;
        Set<String> existing = attributeRepo.findAll().stream()
                .map(AttributeDefinition::getName).collect(Collectors.toSet());
        Map<String, EntityDefinition> entitiesByName = entityRepo.findAll().stream()
                .collect(Collectors.toMap(EntityDefinition::getName, e -> e, (a, b) -> a));
        int skipped = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String name = str(row, 1);
            if (blank(name)) continue;
            if (existing.contains(name)) {
                warnings.add("Attribute skipped (duplicate): " + name);
                skipped++;
                continue;
            }
            String entityName = str(row, 4);
            EntityDefinition entity = blank(entityName) ? null : entitiesByName.get(entityName);
            if (!blank(entityName) && entity == null) {
                warnings.add("Attribute '" + name + "': entity not found '" + entityName + "', imported without entity link");
            }
            attributeRepo.save(AttributeDefinition.builder()
                    .name(name).description(str(row, 2)).entity(entity).build());
            existing.add(name);
            c.attributes++;
        }
        return skipped;
    }

    private int importDatabaseModels(XSSFWorkbook wb, ImportCounts c, List<String> warnings) {
        // col: 0=ID, 1=Name, 2=Description, 3=DB Type, 4=Version,
        //      5=JDBC URL, 6=Username, 7=Schema Pattern, 8=Table Pattern, 9=Import Flags
        Sheet sheet = wb.getSheet("Database Models");
        if (sheet == null) return 0;
        int skipped = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String name = str(row, 1);
            if (blank(name)) continue;
            if (dbModelRepo.existsByName(name)) {
                warnings.add("Database Model skipped (duplicate): " + name);
                skipped++;
                continue;
            }
            DbType dbType = parseEnum(DbType.class, str(row, 3), "DB Type", name, warnings);
            dbModelRepo.save(DatabaseModel.builder()
                    .name(name)
                    .description(str(row, 2))
                    .dbType(dbType != null ? dbType : DbType.GENERIC)
                    .version(str(row, 4))
                    .jdbcUrl(str(row, 5))
                    .username(str(row, 6))
                    .schemaPattern(str(row, 7))
                    .tablePattern(str(row, 8))
                    .importFlags(str(row, 9))
                    .build());
            c.dbModels++;
        }
        return skipped;
    }

    private int importSchemas(XSSFWorkbook wb, ImportCounts c, List<String> warnings) {
        // col: 0=ID, 1=Name, 2=Description, 3=Database Model ID, 4=Database Model
        Sheet sheet = wb.getSheet("Schemas");
        if (sheet == null) return 0;
        // Build name→entity map for database models
        Map<String, DatabaseModel> dbModelsByName = dbModelRepo.findAll().stream()
                .collect(Collectors.toMap(DatabaseModel::getName, m -> m, (a, b) -> a));
        int skipped = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String name = str(row, 1);
            String dbModelName = str(row, 4);
            if (blank(name) || blank(dbModelName)) continue;
            DatabaseModel dbModel = dbModelsByName.get(dbModelName);
            if (dbModel == null) {
                warnings.add("Schema skipped (database model not found: " + dbModelName + "): " + name);
                skipped++;
                continue;
            }
            if (schemaRepo.existsByDatabaseModelIdAndName(dbModel.getId(), name)) {
                warnings.add("Schema skipped (duplicate in model " + dbModelName + "): " + name);
                skipped++;
                continue;
            }
            schemaRepo.save(SchemaDefinition.builder()
                    .name(name).description(str(row, 2)).databaseModel(dbModel).build());
            c.schemas++;
        }
        return skipped;
    }

    private int importTables(XSSFWorkbook wb, ImportCounts c, List<String> warnings) {
        // col: 0=ID, 1=Name, 2=Description, 3=Schema ID, 4=Schema,
        //      5=Database Model ID, 6=Database Model, 7=Entity ID, 8=Entity
        Sheet sheet = wb.getSheet("Tables");
        if (sheet == null) return 0;
        Map<String, SchemaDefinition> schemasByKey = schemaRepo.findAll().stream()
                .collect(Collectors.toMap(
                        s -> s.getDatabaseModel().getName() + "/" + s.getName(),
                        s -> s, (a, b) -> a));
        Map<String, EntityDefinition> entitiesByName = entityRepo.findAll().stream()
                .collect(Collectors.toMap(EntityDefinition::getName, e -> e, (a, b) -> a));
        int skipped = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String name = str(row, 1);
            String schemaName = str(row, 4);
            String dbModelName = str(row, 6);
            if (blank(name) || blank(schemaName) || blank(dbModelName)) continue;
            SchemaDefinition schema = schemasByKey.get(dbModelName + "/" + schemaName);
            if (schema == null) {
                warnings.add("Table skipped (schema not found: " + dbModelName + "/" + schemaName + "): " + name);
                skipped++;
                continue;
            }
            if (tableRepo.existsBySchemaIdAndName(schema.getId(), name)) {
                warnings.add("Table skipped (duplicate in schema " + schemaName + "): " + name);
                skipped++;
                continue;
            }
            String entityName = str(row, 8);
            EntityDefinition entity = blank(entityName) ? null : entitiesByName.get(entityName);
            tableRepo.save(TableDefinition.builder()
                    .name(name).description(str(row, 2))
                    .schema(schema).entity(entity).build());
            c.tables++;
        }
        return skipped;
    }

    private int importColumns(XSSFWorkbook wb, ImportCounts c, List<String> warnings) {
        // col: 0=ID, 1=Name, 2=Data Type, 3=Length, 4=Precision, 5=Scale,
        //      6=Nullable, 7=Primary Key, 8=Unique, 9=Position, 10=Default Value,
        //      11=Description, 12=Table ID, 13=Table, 14=Schema, 15=Database Model, 16=Attribute
        Sheet sheet = wb.getSheet("Columns");
        if (sheet == null) return 0;
        Map<String, TableDefinition> tablesByKey = tableRepo.findAll().stream()
                .collect(Collectors.toMap(
                        t -> t.getSchema().getDatabaseModel().getName()
                                + "/" + t.getSchema().getName()
                                + "/" + t.getName(),
                        t -> t, (a, b) -> a));
        Map<String, AttributeDefinition> attributesByName = attributeRepo.findAll().stream()
                .collect(Collectors.toMap(AttributeDefinition::getName, a -> a, (a, b) -> a));
        int skipped = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String name = str(row, 1);
            String dataType = str(row, 2);
            String tableName = str(row, 13);
            String schemaName = str(row, 14);
            String dbModelName = str(row, 15);
            if (blank(name) || blank(tableName) || blank(schemaName) || blank(dbModelName)) continue;
            TableDefinition table = tablesByKey.get(dbModelName + "/" + schemaName + "/" + tableName);
            if (table == null) {
                warnings.add("Column skipped (table not found: " + tableName + "): " + name);
                skipped++;
                continue;
            }
            if (columnRepo.existsByTableIdAndName(table.getId(), name)) {
                warnings.add("Column skipped (duplicate in table " + tableName + "): " + name);
                skipped++;
                continue;
            }
            String attributeName = str(row, 16);
            AttributeDefinition attribute = blank(attributeName) ? null : attributesByName.get(attributeName);
            if (!blank(attributeName) && attribute == null) {
                warnings.add("Column '" + name + "': attribute not found '" + attributeName + "', imported without attribute link");
            }
            columnRepo.save(ColumnDefinition.builder()
                    .name(name)
                    .dataType(dataType != null ? dataType : "VARCHAR")
                    .length(intVal(row, 3))
                    .precision(intVal(row, 4))
                    .scale(intVal(row, 5))
                    .nullable(boolVal(row, 6))
                    .primaryKey(boolVal(row, 7))
                    .unique(boolVal(row, 8))
                    .ordinalPosition(intVal(row, 9))
                    .defaultValue(str(row, 10))
                    .description(str(row, 11))
                    .table(table)
                    .attribute(attribute)
                    .build());
            c.columns++;
        }
        return skipped;
    }

    private int importRelationships(XSSFWorkbook wb, ImportCounts c, List<String> warnings) {
        // col: 0=ID, 1=Name, 2=Type, 3=Description,
        //      4=From Table ID, 5=From Table, 6=From Column,
        //      7=To Table ID, 8=To Table, 9=To Column
        Sheet sheet = wb.getSheet("Relationships");
        if (sheet == null) return 0;
        Map<String, TableDefinition> tablesByKey = tableRepo.findAll().stream()
                .collect(Collectors.toMap(
                        t -> t.getSchema().getDatabaseModel().getName()
                                + "/" + t.getSchema().getName()
                                + "/" + t.getName(),
                        t -> t, (a, b) -> a));
        // Build a from+to lookup to avoid exact duplicates
        Set<String> existingRels = relationshipRepo.findAll().stream()
                .map(r -> r.getFromTable().getId() + "." + r.getFromColumnName()
                        + "->" + r.getToTable().getId() + "." + r.getToColumnName())
                .collect(Collectors.toSet());
        int skipped = 0;
        for (int i = 1; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;
            String name = str(row, 1);
            if (blank(name)) continue;
            RelationshipType type = parseEnum(RelationshipType.class, str(row, 2), "Type", name, warnings);
            if (type == null) { skipped++; continue; }
            String fromTableName = str(row, 5);
            String fromColumn = str(row, 6);
            String toTableName = str(row, 8);
            String toColumn = str(row, 9);
            // Tables sheet only has table name; we need to find by name (may be ambiguous)
            TableDefinition fromTable = findTableBySimpleName(tablesByKey, fromTableName, name, "from", warnings);
            TableDefinition toTable = findTableBySimpleName(tablesByKey, toTableName, name, "to", warnings);
            if (fromTable == null || toTable == null) { skipped++; continue; }
            String key = fromTable.getId() + "." + fromColumn + "->" + toTable.getId() + "." + toColumn;
            if (existingRels.contains(key)) {
                warnings.add("Relationship skipped (duplicate): " + name);
                skipped++;
                continue;
            }
            relationshipRepo.save(RelationshipDefinition.builder()
                    .name(name).description(str(row, 3)).type(type)
                    .fromTable(fromTable).fromColumnName(fromColumn)
                    .toTable(toTable).toColumnName(toColumn)
                    .build());
            existingRels.add(key);
            c.relationships++;
        }
        return skipped;
    }

    // ---- helpers ----

    private TableDefinition findTableBySimpleName(Map<String, TableDefinition> tablesByKey,
                                                   String tableName, String relName,
                                                   String side, List<String> warnings) {
        if (blank(tableName)) {
            warnings.add("Relationship skipped (missing " + side + " table): " + relName);
            return null;
        }
        // Key format is dbModel/schema/table — find first match by table name suffix
        List<TableDefinition> matches = tablesByKey.entrySet().stream()
                .filter(e -> e.getKey().endsWith("/" + tableName))
                .map(Map.Entry::getValue)
                .toList();
        if (matches.isEmpty()) {
            warnings.add("Relationship skipped (" + side + " table not found: " + tableName + "): " + relName);
            return null;
        }
        if (matches.size() > 1) {
            warnings.add("Relationship: ambiguous " + side + " table name '" + tableName
                    + "' — using first match for: " + relName);
        }
        return matches.get(0);
    }

    private <T extends Enum<T>> T parseEnum(Class<T> type, String value, String field,
                                             String rowName, List<String> warnings) {
        if (blank(value)) {
            warnings.add("Missing " + field + " for: " + rowName);
            return null;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            warnings.add("Unknown " + field + " '" + value + "' for: " + rowName);
            return null;
        }
    }

    private String str(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> {
                String v = cell.getStringCellValue().trim();
                yield v.isEmpty() ? null : v;
            }
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private Long longVal(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case NUMERIC -> (long) cell.getNumericCellValue();
            case STRING -> {
                try { yield Long.parseLong(cell.getStringCellValue().trim()); }
                catch (NumberFormatException e) { yield null; }
            }
            default -> null;
        };
    }

    private Integer intVal(Row row, int col) {
        Long l = longVal(row, col);
        return l != null ? l.intValue() : null;
    }

    private boolean boolVal(Row row, int col) {
        Cell cell = row.getCell(col, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return false;
        return switch (cell.getCellType()) {
            case BOOLEAN -> cell.getBooleanCellValue();
            case STRING -> Boolean.parseBoolean(cell.getStringCellValue().trim());
            case NUMERIC -> cell.getNumericCellValue() != 0;
            default -> false;
        };
    }

    private boolean blank(String s) {
        return s == null || s.isBlank();
    }

    private static class ImportCounts {
        int entities, attributes, dbModels, schemas, tables, columns, relationships;
    }
}
