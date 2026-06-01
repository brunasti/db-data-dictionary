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
public class AlignmentResult {
    private Long databaseModelId;
    private String databaseModelName;
    private String jdbcUrl;
    private boolean aligned;

    @Builder.Default
    private int schemasChecked = 0;

    @Builder.Default
    private int tablesChecked = 0;

    @Builder.Default
    private int columnsChecked = 0;

    @Builder.Default
    private List<AlignmentItem> differences = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
