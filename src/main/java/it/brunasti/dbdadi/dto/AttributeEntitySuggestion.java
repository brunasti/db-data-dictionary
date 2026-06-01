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
public class AttributeEntitySuggestion {
    private Long entityId;
    private String entityName;
    private String entityDescription;
    /** Tables whose columns reference this attribute and are linked to this entity. */
    private List<String> viaTableNames;
    private int linkedColumnsCount;
}
