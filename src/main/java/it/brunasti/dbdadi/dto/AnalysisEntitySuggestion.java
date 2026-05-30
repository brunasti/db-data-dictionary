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
public class AnalysisEntitySuggestion {

    private String suggestedName;
    private Long existingEntityId;   // null if no entity with this name exists yet
    private List<Long> tableIds;
    private List<String> tableLabels; // "DatabaseModel / Schema / Table"
}
