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
public class BulkEntityResult {
    @Builder.Default
    private int entitiesCreated = 0;

    @Builder.Default
    private int entitiesReused = 0;

    @Builder.Default
    private int tablesLinked = 0;

    @Builder.Default
    private int tablesSkipped = 0;

    @Builder.Default
    private List<String> createdNames = new ArrayList<>();

    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
