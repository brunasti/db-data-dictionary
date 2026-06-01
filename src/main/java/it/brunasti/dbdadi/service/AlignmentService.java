package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.AlignmentItem;
import it.brunasti.dbdadi.dto.AlignmentRequest;
import it.brunasti.dbdadi.dto.AlignmentResult;
import it.brunasti.dbdadi.model.ColumnDefinition;
import it.brunasti.dbdadi.model.DatabaseModel;
import it.brunasti.dbdadi.model.SchemaDefinition;
import it.brunasti.dbdadi.model.TableDefinition;
import it.brunasti.dbdadi.repository.ColumnDefinitionRepository;
import it.brunasti.dbdadi.repository.DatabaseModelRepository;
import it.brunasti.dbdadi.repository.SchemaDefinitionRepository;
import it.brunasti.dbdadi.repository.TableDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlignmentService {

    private final DatabaseModelRepository dbModelRepo;
    private final SchemaDefinitionRepository schemaRepo;
    private final TableDefinitionRepository tableRepo;
    private final ColumnDefinitionRepository columnRepo;

    @Transactional(readOnly = true)
    public AlignmentResult check(AlignmentRequest request) {
        DatabaseModel model = dbModelRepo.findById(request.getDatabaseModelId())
                .orElseThrow(() -> new RuntimeException("DatabaseModel not found: " + request.getDatabaseModelId()));

        if (model.getJdbcUrl() == null || model.getJdbcUrl().isBlank()) {
            throw new RuntimeException("DatabaseModel '" + model.getName() + "' has no JDBC URL stored. "
                    + "Alignment is only available for JDBC-imported models.");
        }

        List<AlignmentItem> differences = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int schemasChecked = 0, tablesChecked = 0, columnsChecked = 0;

        List<SchemaDefinition> storedSchemas = schemaRepo.findByDatabaseModelId(model.getId());
        Map<String, SchemaDefinition> storedSchemaMap = storedSchemas.stream()
                .collect(Collectors.toMap(s -> s.getName().toUpperCase(), s -> s));

        String tablePattern = (model.getTablePattern() != null && !model.getTablePattern().isBlank())
                ? model.getTablePattern() : "%";
        boolean includeViews = model.getImportFlags() != null && model.getImportFlags().contains("includeViews");
        String[] tableTypes = includeViews ? new String[]{"TABLE", "VIEW"} : new String[]{"TABLE"};

        try (Connection conn = DriverManager.getConnection(
                model.getJdbcUrl(), model.getUsername(), request.getPassword())) {

            DatabaseMetaData meta = conn.getMetaData();
            log.info("Alignment check connected to {} {}", meta.getDatabaseProductName(), meta.getDatabaseProductVersion());

            List<String> liveSchemas = resolveSchemas(meta, model.getSchemaPattern());
            schemasChecked = liveSchemas.size();

            Set<String> liveSchemaKeys = liveSchemas.stream()
                    .map(String::toUpperCase).collect(Collectors.toSet());

            // Schemas in stored model but gone from live DB
            for (String key : storedSchemaMap.keySet()) {
                if (!liveSchemaKeys.contains(key)) {
                    differences.add(AlignmentItem.builder()
                            .schemaName(key).status("REMOVED")
                            .details("Schema exists in model but not in live database").build());
                }
            }

            // Schemas in live DB but not yet in stored model
            for (String liveSchema : liveSchemas) {
                if (!storedSchemaMap.containsKey(liveSchema.toUpperCase())) {
                    differences.add(AlignmentItem.builder()
                            .schemaName(liveSchema).status("ADDED")
                            .details("Schema exists in live database but not in model").build());
                }
            }

            // Compare tables and columns for matching schemas
            for (String liveSchema : liveSchemas) {
                SchemaDefinition storedSchema = storedSchemaMap.get(liveSchema.toUpperCase());
                if (storedSchema == null) continue;

                List<TableDefinition> storedTables = tableRepo.findBySchemaId(storedSchema.getId());
                Map<String, TableDefinition> storedTableMap = storedTables.stream()
                        .collect(Collectors.toMap(t -> t.getName().toUpperCase(), t -> t));

                Map<String, String> liveTableNames = new LinkedHashMap<>(); // upper -> original
                try (ResultSet rs = meta.getTables(null, liveSchema, tablePattern, tableTypes)) {
                    while (rs.next()) {
                        String n = rs.getString("TABLE_NAME");
                        liveTableNames.put(n.toUpperCase(), n);
                    }
                }

                tablesChecked += liveTableNames.size();

                // Tables removed
                for (String key : storedTableMap.keySet()) {
                    if (!liveTableNames.containsKey(key)) {
                        differences.add(AlignmentItem.builder()
                                .schemaName(liveSchema).tableName(storedTableMap.get(key).getName())
                                .status("REMOVED")
                                .details("Table exists in model but not in live database").build());
                    }
                }

                // Tables added
                for (String upperKey : liveTableNames.keySet()) {
                    if (!storedTableMap.containsKey(upperKey)) {
                        differences.add(AlignmentItem.builder()
                                .schemaName(liveSchema).tableName(liveTableNames.get(upperKey))
                                .status("ADDED")
                                .details("Table exists in live database but not in model").build());
                    }
                }

                // Compare columns for matching tables
                for (Map.Entry<String, String> liveEntry : liveTableNames.entrySet()) {
                    TableDefinition storedTable = storedTableMap.get(liveEntry.getKey());
                    if (storedTable == null) continue;

                    String liveTableName = liveEntry.getValue();

                    List<ColumnDefinition> storedCols = columnRepo.findByTableIdOrderByOrdinalPosition(storedTable.getId());
                    Map<String, ColumnDefinition> storedColMap = storedCols.stream()
                            .collect(Collectors.toMap(c -> c.getName().toUpperCase(), c -> c));

                    Set<String> pkColumns = new HashSet<>();
                    try (ResultSet pk = meta.getPrimaryKeys(null, liveSchema, liveTableName)) {
                        while (pk.next()) pkColumns.add(pk.getString("COLUMN_NAME").toUpperCase());
                    }

                    Map<String, LiveColumn> liveColMap = new LinkedHashMap<>();
                    try (ResultSet rs = meta.getColumns(null, liveSchema, liveTableName, "%")) {
                        while (rs.next()) {
                            String colName = rs.getString("COLUMN_NAME");
                            String colType = rs.getString("TYPE_NAME");
                            boolean nullable = rs.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
                            boolean isPk = pkColumns.contains(colName.toUpperCase());
                            liveColMap.put(colName.toUpperCase(), new LiveColumn(colName, colType, nullable, isPk));
                        }
                    }

                    columnsChecked += liveColMap.size();

                    // Columns removed
                    for (Map.Entry<String, ColumnDefinition> e : storedColMap.entrySet()) {
                        if (!liveColMap.containsKey(e.getKey())) {
                            differences.add(AlignmentItem.builder()
                                    .schemaName(liveSchema).tableName(liveTableName)
                                    .columnName(e.getValue().getName()).status("REMOVED")
                                    .details("Column exists in model but not in live database").build());
                        }
                    }

                    // Columns added or changed
                    for (Map.Entry<String, LiveColumn> e : liveColMap.entrySet()) {
                        LiveColumn live = e.getValue();
                        ColumnDefinition stored = storedColMap.get(e.getKey());

                        if (stored == null) {
                            differences.add(AlignmentItem.builder()
                                    .schemaName(liveSchema).tableName(liveTableName)
                                    .columnName(live.name).status("ADDED")
                                    .details("Column exists in live database but not in model").build());
                        } else {
                            List<String> changes = new ArrayList<>();
                            if (!live.type.equalsIgnoreCase(stored.getDataType())) {
                                changes.add("type: " + stored.getDataType() + " → " + live.type);
                            }
                            if (live.nullable != stored.isNullable()) {
                                changes.add("nullable: " + stored.isNullable() + " → " + live.nullable);
                            }
                            if (live.primaryKey != stored.isPrimaryKey()) {
                                changes.add("primaryKey: " + stored.isPrimaryKey() + " → " + live.primaryKey);
                            }
                            if (!changes.isEmpty()) {
                                differences.add(AlignmentItem.builder()
                                        .schemaName(liveSchema).tableName(liveTableName)
                                        .columnName(live.name).status("CHANGED")
                                        .details(String.join("; ", changes)).build());
                            }
                        }
                    }
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("JDBC connection failed: " + e.getMessage(), e);
        }

        return AlignmentResult.builder()
                .databaseModelId(model.getId())
                .databaseModelName(model.getName())
                .jdbcUrl(model.getJdbcUrl())
                .aligned(differences.isEmpty())
                .schemasChecked(schemasChecked)
                .tablesChecked(tablesChecked)
                .columnsChecked(columnsChecked)
                .differences(differences)
                .warnings(warnings)
                .build();
    }

    // Same schema-resolution logic as JdbcImportService
    private List<String> resolveSchemas(DatabaseMetaData meta, String schemaPattern) throws SQLException {
        List<String> schemas = new ArrayList<>();
        String pattern = (schemaPattern == null || schemaPattern.isBlank()) ? null : schemaPattern;

        try (ResultSet rs = meta.getSchemas(null, pattern)) {
            while (rs.next()) {
                String name = rs.getString("TABLE_SCHEM");
                if (name != null && !isSystemSchema(name)) schemas.add(name);
            }
        }

        if (schemas.isEmpty()) {
            try (ResultSet rs = meta.getTables(null, pattern, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    String schem = rs.getString("TABLE_SCHEM");
                    if (schem == null) schem = rs.getString("TABLE_CAT");
                    if (schem != null && !schemas.contains(schem) && !isSystemSchema(schem)) schemas.add(schem);
                }
            }
        }

        if (schemas.isEmpty()) schemas.add("default");
        return schemas;
    }

    private boolean isSystemSchema(String name) {
        if (name == null) return false;
        String n = name.toLowerCase();
        return n.startsWith("pg_") || n.equals("information_schema") || n.equals("sys")
                || n.equals("system") || n.equals("sysibm") || n.equals("syscat") || n.equals("sysstat");
    }

    private record LiveColumn(String name, String type, boolean nullable, boolean primaryKey) {}
}
