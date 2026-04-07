package it.brunasti.dbdadi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JdbcImportResult {

    private Long databaseModelId;
    private String databaseModelName;

    @Builder.Default
    private int schemasImported = 0;

    @Builder.Default
    private int tablesImported = 0;

    @Builder.Default
    private int columnsImported = 0;

    @Builder.Default
    private int relationshipsImported = 0;

    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
