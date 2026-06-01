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
public class MergeEntityResult {
    private Long survivingEntityId;
    private String survivingEntityName;

    @Builder.Default private int attributesMigrated = 0;
    @Builder.Default private int tablesMigrated = 0;
    @Builder.Default private int domainsMigrated = 0;

    @Builder.Default private List<String> warnings = new ArrayList<>();
}
