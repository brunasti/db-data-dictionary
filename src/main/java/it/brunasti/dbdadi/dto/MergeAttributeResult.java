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
public class MergeAttributeResult {
    private Long survivingAttributeId;
    private String survivingAttributeName;
    private int columnsMigrated;
    private List<String> warnings;
}
