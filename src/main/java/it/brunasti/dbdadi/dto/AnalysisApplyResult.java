package it.brunasti.dbdadi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisApplyResult {

    private int entitiesCreated;
    private int entitiesReused;
    private int tablesLinked;
    private int attributesCreated;
    private int attributesReused;
    private int columnsLinked;
}
