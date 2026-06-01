package it.brunasti.dbdadi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlignmentItem {
    private String schemaName;
    private String tableName;   // null for schema-level items
    private String columnName;  // null for schema/table-level items
    private String status;      // ADDED | REMOVED | CHANGED
    private String details;
}
