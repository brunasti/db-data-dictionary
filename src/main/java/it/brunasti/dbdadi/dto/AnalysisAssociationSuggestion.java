package it.brunasti.dbdadi.dto;

import it.brunasti.dbdadi.model.enums.RelationshipType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisAssociationSuggestion {

    private String suggestedName;
    private RelationshipType type;
    private Long fromEntityId;
    private String fromEntityName;
    private Long toEntityId;
    private String toEntityName;
    private Long existingAssociationId;   // null if no association exists yet
    private List<String> relationshipLabels; // backing physical relationships
}
