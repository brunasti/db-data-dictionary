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
public class AnalysisAttributeSuggestion {

    private String suggestedName;
    private String entityName;          // which entity group this belongs to
    private Long existingAttributeId;   // null if no attribute with this name exists yet under that entity
    private List<Long> columnIds;
    private List<String> columnLabels;  // "DatabaseModel / Schema / Table / Column"
}
