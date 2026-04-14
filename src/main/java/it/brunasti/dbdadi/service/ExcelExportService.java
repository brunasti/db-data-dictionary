package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.model.*;
import it.brunasti.dbdadi.model.enums.UserRole;
import it.brunasti.dbdadi.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExcelExportService {

    private final EntityDefinitionRepository entityRepo;
    private final AttributeDefinitionRepository attributeRepo;
    private final DatabaseModelRepository dbModelRepo;
    private final SchemaDefinitionRepository schemaRepo;
    private final TableDefinitionRepository tableRepo;
    private final ColumnDefinitionRepository columnRepo;
    private final RelationshipDefinitionRepository relationshipRepo;
    private final UserRepository userRepo;

    public byte[] exportAll() throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            CellStyle header = headerStyle(wb);

            writeSheet(wb, header, "Entities",
                    new String[]{"ID", "Name", "Description"},
                    entityRepo.findAll(), this::entityRow);

            writeSheet(wb, header, "Attributes",
                    new String[]{"ID", "Name", "Description", "Entity ID", "Entity"},
                    attributeRepo.findAll(), this::attributeRow);

            writeSheet(wb, header, "Database Models",
                    new String[]{"ID", "Name", "Description", "DB Type", "Version",
                                 "JDBC URL", "Username", "Schema Pattern", "Table Pattern", "Import Flags"},
                    dbModelRepo.findAll(), this::dbModelRow);

            writeSheet(wb, header, "Schemas",
                    new String[]{"ID", "Name", "Description", "Database Model ID", "Database Model"},
                    schemaRepo.findAll(), this::schemaRow);

            writeSheet(wb, header, "Tables",
                    new String[]{"ID", "Name", "Description", "Schema ID", "Schema",
                                 "Database Model ID", "Database Model", "Entity ID", "Entity"},
                    tableRepo.findAll(), this::tableRow);

            writeSheet(wb, header, "Columns",
                    new String[]{"ID", "Name", "Data Type", "Length", "Precision", "Scale",
                                 "Nullable", "Primary Key", "Unique", "Position",
                                 "Default Value", "Description",
                                 "Table ID", "Table", "Schema", "Database Model", "Attribute"},
                    columnRepo.findAll(), this::columnRow);

            writeSheet(wb, header, "Relationships",
                    new String[]{"ID", "Name", "Type", "Description",
                                 "From Table ID", "From Table", "From Column",
                                 "To Table ID", "To Table", "To Column"},
                    relationshipRepo.findAll(), this::relationshipRow);

            writeSheet(wb, header, "Users",
                    new String[]{"ID", "Username", "Password Hash", "Role", "Enabled"},
                    userRepo.findAll(), this::userRow);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }

    // --- sheet writers ---

    @FunctionalInterface
    private interface RowMapper<T> {
        Object[] map(T entity);
    }

    private <T> void writeSheet(XSSFWorkbook wb, CellStyle headerStyle,
                                 String sheetName, String[] headers,
                                 List<T> rows, RowMapper<T> mapper) {
        Sheet sheet = wb.createSheet(sheetName);
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        int rowIdx = 1;
        for (T item : rows) {
            Object[] values = mapper.map(item);
            Row row = sheet.createRow(rowIdx++);
            for (int i = 0; i < values.length; i++) {
                Cell cell = row.createCell(i);
                if (values[i] == null) {
                    cell.setCellValue("");
                } else if (values[i] instanceof Boolean b) {
                    cell.setCellValue(b);
                } else if (values[i] instanceof Number n) {
                    cell.setCellValue(n.doubleValue());
                } else {
                    cell.setCellValue(values[i].toString());
                }
            }
        }
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    // --- row mappers ---

    private Object[] dbModelRow(DatabaseModel e) {
        return new Object[]{e.getId(), e.getName(), e.getDescription(),
                e.getDbType() != null ? e.getDbType().name() : null,
                e.getVersion(), e.getJdbcUrl(), e.getUsername(),
                e.getSchemaPattern(), e.getTablePattern(), e.getImportFlags()};
    }

    private Object[] entityRow(EntityDefinition e) {
        return new Object[]{e.getId(), e.getName(), e.getDescription()};
    }

    private Object[] attributeRow(AttributeDefinition e) {
        return new Object[]{e.getId(), e.getName(), e.getDescription(),
                e.getEntity() != null ? e.getEntity().getId() : null,
                e.getEntity() != null ? e.getEntity().getName() : null};
    }

    private Object[] schemaRow(SchemaDefinition e) {
        return new Object[]{e.getId(), e.getName(), e.getDescription(),
                e.getDatabaseModel().getId(), e.getDatabaseModel().getName()};
    }

    private Object[] tableRow(TableDefinition e) {
        return new Object[]{e.getId(), e.getName(), e.getDescription(),
                e.getSchema().getId(), e.getSchema().getName(),
                e.getSchema().getDatabaseModel().getId(), e.getSchema().getDatabaseModel().getName(),
                e.getEntity() != null ? e.getEntity().getId() : null,
                e.getEntity() != null ? e.getEntity().getName() : null};
    }

    private Object[] columnRow(ColumnDefinition e) {
        return new Object[]{e.getId(), e.getName(), e.getDataType(),
                e.getLength(), e.getPrecision(), e.getScale(),
                e.isNullable(), e.isPrimaryKey(), e.isUnique(),
                e.getOrdinalPosition(), e.getDefaultValue(), e.getDescription(),
                e.getTable().getId(), e.getTable().getName(),
                e.getTable().getSchema().getName(),
                e.getTable().getSchema().getDatabaseModel().getName(),
                e.getAttribute() != null ? e.getAttribute().getName() : null};
    }

    private Object[] userRow(User e) {
        return new Object[]{e.getId(), e.getUsername(), e.getPasswordHash(),
                e.getRole() != null ? e.getRole().name() : null, e.isEnabled()};
    }

    private Object[] relationshipRow(RelationshipDefinition e) {
        return new Object[]{e.getId(), e.getName(),
                e.getType() != null ? e.getType().name() : null,
                e.getDescription(),
                e.getFromTable().getId(), e.getFromTable().getName(), e.getFromColumnName(),
                e.getToTable().getId(), e.getToTable().getName(), e.getToColumnName()};
    }

    // --- style ---

    private CellStyle headerStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }
}
