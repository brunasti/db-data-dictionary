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
public class GenerateAssociationsResult {

    private int associationsCreated;
    private int associationsSkipped;
    private List<String> createdNames;
    private List<String> warnings;
}
