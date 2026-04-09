package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.JdbcImportRequest;
import it.brunasti.dbdadi.dto.JdbcImportResult;
import it.brunasti.dbdadi.model.*;
import it.brunasti.dbdadi.model.enums.DbType;
import it.brunasti.dbdadi.model.enums.RelationshipType;
import it.brunasti.dbdadi.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class JdbcImportService {

    private final DatabaseModelRepository dbModelRepo;
    private final SchemaDefinitionRepository schemaRepo;
    private final TableDefinitionRepository tableRepo;
    private final ColumnDefinitionRepository columnRepo;
    private final RelationshipDefinitionRepository relationshipRepo;

    @Transactional
    public JdbcImportResult importFromJdbc(JdbcImportRequest request) {
        JdbcImportResult result = new JdbcImportResult();

        // 1. Create or overwrite the DatabaseModel entry
        DatabaseModel dbModel = prepareModel(request, result);

        // 2. Connect and introspect
        try (Connection conn = DriverManager.getConnection(
                request.getJdbcUrl(),
                request.getUsername(),
                request.getPassword())) {

            DatabaseMetaData meta = conn.getMetaData();
            log.info("Connected to {} {}", meta.getDatabaseProductName(), meta.getDatabaseProductVersion());

            String tablePattern = nullIfBlank(request.getTablePattern(), "%");
            String[] tableTypes = request.isIncludeViews()
                    ? new String[]{"TABLE", "VIEW"}
                    : new String[]{"TABLE"};

            // 3. Collect schemas
            List<String> schemaNames = resolveSchemas(meta, request.getSchemaPattern());
            if (schemaNames.isEmpty()) {
                result.getWarnings().add("No schemas found matching pattern: " + request.getSchemaPattern());
                return result;
            }

            // 4. For each schema: import tables and columns
            // tableKey → TableDefinition, for relationship wiring
            Map<String, TableDefinition> tableMap = new LinkedHashMap<>();

            for (String schemaName : schemaNames) {
                SchemaDefinition schema = SchemaDefinition.builder()
                        .name(schemaName)
                        .databaseModel(dbModel)
                        .build();
                schema = schemaRepo.save(schema);
                result.setSchemasImported(result.getSchemasImported() + 1);

                importTablesForSchema(meta, schema, schemaName, tablePattern, tableTypes, tableMap, result);
            }

            // 5. Import foreign key relationships
            for (Map.Entry<String, TableDefinition> entry : tableMap.entrySet()) {
                String[] parts = entry.getKey().split("\\.", 2);
                String schemaName = parts[0];
                String tableName  = parts[1];
                importRelationships(meta, schemaName, tableName, entry.getValue(), tableMap, result);
            }

        } catch (SQLException e) {
            throw new RuntimeException("JDBC connection failed: " + e.getMessage(), e);
        }

        result.setDatabaseModelId(dbModel.getId());
        result.setDatabaseModelName(dbModel.getName());
        return result;
    }

    // --- private helpers ---

    private DatabaseModel prepareModel(JdbcImportRequest request, JdbcImportResult result) {
        Optional<DatabaseModel> existing = dbModelRepo.findByName(request.getModelName());
        if (existing.isPresent()) {
            if (request.isOverwrite()) {
                log.info("Overwriting existing model '{}'", request.getModelName());
                dbModelRepo.delete(existing.get());
                dbModelRepo.flush();
            } else {
                result.getWarnings().add("Model '" + request.getModelName()
                        + "' already exists. Use overwrite=true to replace it.");
                return existing.get();
            }
        }

        DbType dbType = detectDbType(request.getJdbcUrl());
        DatabaseModel model = DatabaseModel.builder()
                .name(request.getModelName())
                .dbType(dbType)
                .description("Imported via JDBC from " + request.getJdbcUrl())
                .jdbcUrl(request.getJdbcUrl())
                .username(request.getUsername())
                .schemaPattern(request.getSchemaPattern())
                .tablePattern(request.getTablePattern())
                .importFlags((request.isIncludeViews() ? "includeViews" : "") +
                             (request.isOverwrite() ? " overwrite" : "").trim())
                .build();
        return dbModelRepo.save(model);
    }

    private List<String> resolveSchemas(DatabaseMetaData meta, String schemaPattern) throws SQLException {
        List<String> schemas = new ArrayList<>();
        String pattern = nullIfBlank(schemaPattern, null);

        // Standard getSchemas() — works for PostgreSQL, H2, Oracle, DB2
        try (ResultSet rs = meta.getSchemas(null, pattern)) {
            while (rs.next()) {
                String name = rs.getString("TABLE_SCHEM");
                if (name != null && !isSystemSchema(name)) {
                    schemas.add(name);
                }
            }
        }

        // Fallback for MySQL (uses catalogs, getSchemas returns empty)
        if (schemas.isEmpty()) {
            try (ResultSet rs = meta.getTables(null, pattern, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String schem = rs.getString("TABLE_SCHEM");
                    if (schem == null) schem = rs.getString("TABLE_CAT");
                    if (schem != null && !schemas.contains(schem) && !isSystemSchema(schem)) {
                        schemas.add(schem);
                    }
                }
            }
        }

        // Last resort: use a synthetic "default" schema
        if (schemas.isEmpty()) {
            schemas.add("default");
        }

        return schemas;
    }

    private void importTablesForSchema(DatabaseMetaData meta,
                                       SchemaDefinition schema,
                                       String schemaName,
                                       String tablePattern,
                                       String[] tableTypes,
                                       Map<String, TableDefinition> tableMap,
                                       JdbcImportResult result) throws SQLException {

        try (ResultSet rs = meta.getTables(null, schemaName, tablePattern, tableTypes)) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                String remarks   = rs.getString("REMARKS");

                TableDefinition table = TableDefinition.builder()
                        .name(tableName)
                        .description(remarks)
                        .schema(schema)
                        .build();
                table = tableRepo.save(table);
                result.setTablesImported(result.getTablesImported() + 1);
                tableMap.put(schemaName + "." + tableName, table);

                importColumns(meta, schemaName, tableName, table, result);
            }
        }
    }

    private void importColumns(DatabaseMetaData meta,
                                String schemaName,
                                String tableName,
                                TableDefinition table,
                                JdbcImportResult result) throws SQLException {

        // Collect primary key column names
        Set<String> pkColumns = new HashSet<>();
        try (ResultSet pk = meta.getPrimaryKeys(null, schemaName, tableName)) {
            while (pk.next()) {
                pkColumns.add(pk.getString("COLUMN_NAME"));
            }
        }

        try (ResultSet rs = meta.getColumns(null, schemaName, tableName, "%")) {
            while (rs.next()) {
                String colName      = rs.getString("COLUMN_NAME");
                String typeName     = rs.getString("TYPE_NAME");
                int    columnSize   = rs.getInt("COLUMN_SIZE");
                int    decimalDigits= rs.getInt("DECIMAL_DIGITS");
                int    nullable     = rs.getInt("NULLABLE");
                String defaultVal   = rs.getString("COLUMN_DEF");
                int    ordinal      = rs.getInt("ORDINAL_POSITION");
                String remarks      = rs.getString("REMARKS");

                boolean isPk = pkColumns.contains(colName);

                // Separate length vs precision based on type
                Integer length    = isNumericType(typeName) ? null : columnSize;
                Integer precision = isNumericType(typeName) ? columnSize : null;
                Integer scale     = decimalDigits > 0 ? decimalDigits : null;

                ColumnDefinition col = ColumnDefinition.builder()
                        .name(colName)
                        .dataType(typeName)
                        .length(length)
                        .precision(precision)
                        .scale(scale)
                        .nullable(nullable != DatabaseMetaData.columnNoNulls)
                        .primaryKey(isPk)
                        .defaultValue(defaultVal)
                        .ordinalPosition(ordinal)
                        .description(remarks)
                        .table(table)
                        .build();
                columnRepo.save(col);
                result.setColumnsImported(result.getColumnsImported() + 1);
            }
        }
    }

    private void importRelationships(DatabaseMetaData meta,
                                     String schemaName,
                                     String tableName,
                                     TableDefinition fromTable,
                                     Map<String, TableDefinition> tableMap,
                                     JdbcImportResult result) throws SQLException {

        // getImportedKeys returns FKs defined on this table pointing to other tables
        Map<String, FkInfo> fkMap = new LinkedHashMap<>();

        try (ResultSet rs = meta.getImportedKeys(null, schemaName, tableName)) {
            while (rs.next()) {
                String fkName       = rs.getString("FK_NAME");
                String pkSchema     = rs.getString("PKTABLE_SCHEM");
                String pkTable      = rs.getString("PKTABLE_NAME");
                String pkCol        = rs.getString("PKCOLUMN_NAME");
                String fkCol        = rs.getString("FKCOLUMN_NAME");
                short  keySeq       = rs.getShort("KEY_SEQ");

                if (fkName == null) {
                    fkName = "fk_" + tableName + "_" + pkTable + "_" + keySeq;
                }

                fkMap.computeIfAbsent(fkName, k -> new FkInfo(k, pkSchema, pkTable))
                        .addColumns(fkCol, pkCol, keySeq);
            }
        }

        for (FkInfo fk : fkMap.values()) {
            String toKey = fk.pkSchema + "." + fk.pkTable;
            TableDefinition toTable = tableMap.get(toKey);

            if (toTable == null) {
                result.getWarnings().add("Skipped FK '" + fk.name
                        + "': referenced table '" + toKey + "' not in import scope.");
                continue;
            }

            if (fk.fkColumns.size() > 1) {
                result.getWarnings().add("FK '" + fk.name
                        + "' is composite (" + fk.fkColumns + " → " + fk.pkColumns
                        + "). Imported using first column only.");
            }

            RelationshipDefinition rel = RelationshipDefinition.builder()
                    .name(fk.name)
                    .type(RelationshipType.MANY_TO_ONE)
                    .fromTable(fromTable)
                    .fromColumnName(fk.fkColumns.get(0))
                    .toTable(toTable)
                    .toColumnName(fk.pkColumns.get(0))
                    .build();
            relationshipRepo.save(rel);
            result.setRelationshipsImported(result.getRelationshipsImported() + 1);
        }
    }

    // --- utilities ---

    private DbType detectDbType(String url) {
        if (url == null) return DbType.GENERIC;
        String u = url.toLowerCase();
        if (u.startsWith("jdbc:postgresql")) return DbType.POSTGRESQL;
        if (u.startsWith("jdbc:mysql"))      return DbType.MYSQL;
        if (u.startsWith("jdbc:oracle"))     return DbType.ORACLE;
        if (u.startsWith("jdbc:db2"))        return DbType.DB2;
        if (u.startsWith("jdbc:sqlserver"))  return DbType.SQLSERVER;
        if (u.startsWith("jdbc:h2"))         return DbType.H2;
        return DbType.GENERIC;
    }

    private boolean isSystemSchema(String name) {
        if (name == null) return false;
        String n = name.toLowerCase();
        return n.startsWith("pg_")
                || n.equals("information_schema")
                || n.equals("sys")
                || n.equals("system")
                || n.equals("sysibm")
                || n.equals("syscat")
                || n.equals("sysstat");
    }

    private boolean isNumericType(String typeName) {
        if (typeName == null) return false;
        String t = typeName.toUpperCase();
        return t.contains("INT") || t.contains("NUMERIC") || t.contains("DECIMAL")
                || t.contains("FLOAT") || t.contains("DOUBLE") || t.contains("REAL")
                || t.contains("NUMBER");
    }

    private String nullIfBlank(String value, String defaultValue) {
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    /** Internal holder for FK metadata while iterating ResultSet */
    private static class FkInfo {
        final String name;
        final String pkSchema;
        final String pkTable;
        final List<String> fkColumns = new ArrayList<>();
        final List<String> pkColumns = new ArrayList<>();

        FkInfo(String name, String pkSchema, String pkTable) {
            this.name = name;
            this.pkSchema = pkSchema != null ? pkSchema : "default";
            this.pkTable  = pkTable;
        }

        void addColumns(String fkCol, String pkCol, short seq) {
            // KEY_SEQ is 1-based; insert at correct position
            int idx = seq - 1;
            while (fkColumns.size() <= idx) { fkColumns.add(null); pkColumns.add(null); }
            fkColumns.set(idx, fkCol);
            pkColumns.set(idx, pkCol);
        }
    }
}
