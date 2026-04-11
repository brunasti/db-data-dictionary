package it.brunasti.dbdadi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelImportResult {
    private int entitiesImported;
    private int attributesImported;
    private int databaseModelsImported;
    private int schemasImported;
    private int tablesImported;
    private int columnsImported;
    private int relationshipsImported;
    private int skipped;
    private List<String> warnings;
}
