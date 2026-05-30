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
public class AnalysisApplyRequest {

    private List<AnalysisEntitySuggestion> entities;
    private List<AnalysisAttributeSuggestion> attributes;
    private Long domainId; // optional: link created/matched entities to this domain
}
