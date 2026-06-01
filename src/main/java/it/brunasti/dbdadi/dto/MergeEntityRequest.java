package it.brunasti.dbdadi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MergeEntityRequest {
    /** Entity to be absorbed and deleted. */
    private Long sourceEntityId;
    /** Entity that survives and receives all data. */
    private Long targetEntityId;
}
