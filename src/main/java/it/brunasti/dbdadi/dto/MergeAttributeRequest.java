package it.brunasti.dbdadi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeAttributeRequest {
    /** Attribute to be absorbed and deleted. */
    private Long sourceAttributeId;
    /** Attribute that survives and receives all column links. */
    private Long targetAttributeId;
}
