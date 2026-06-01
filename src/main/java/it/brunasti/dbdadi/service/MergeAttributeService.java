package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.MergeAttributeRequest;
import it.brunasti.dbdadi.dto.MergeAttributeResult;
import it.brunasti.dbdadi.exception.ResourceNotFoundException;
import it.brunasti.dbdadi.model.AttributeDefinition;
import it.brunasti.dbdadi.model.ColumnDefinition;
import it.brunasti.dbdadi.repository.AttributeDefinitionRepository;
import it.brunasti.dbdadi.repository.ColumnDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MergeAttributeService {

    private final AttributeDefinitionRepository attributeRepository;
    private final ColumnDefinitionRepository columnRepository;

    @Transactional
    public MergeAttributeResult merge(MergeAttributeRequest request) {
        if (request.getSourceAttributeId() == null || request.getTargetAttributeId() == null) {
            throw new IllegalArgumentException("Both sourceAttributeId and targetAttributeId are required");
        }
        if (request.getSourceAttributeId().equals(request.getTargetAttributeId())) {
            throw new IllegalArgumentException("Source and target attribute must be different");
        }

        AttributeDefinition source = attributeRepository.findById(request.getSourceAttributeId())
                .orElseThrow(() -> new ResourceNotFoundException("AttributeDefinition", request.getSourceAttributeId()));
        AttributeDefinition target = attributeRepository.findById(request.getTargetAttributeId())
                .orElseThrow(() -> new ResourceNotFoundException("AttributeDefinition", request.getTargetAttributeId()));

        List<String> warnings = new ArrayList<>();

        // Move all column links from source to target
        List<ColumnDefinition> columns = columnRepository.findByAttributeId(source.getId());
        for (ColumnDefinition col : columns) {
            col.setAttribute(target);
            columnRepository.save(col);
            log.info("Re-linked column '{}.{}' from attribute '{}' to '{}'",
                    col.getTable() != null ? col.getTable().getName() : "?",
                    col.getName(), source.getName(), target.getName());
        }
        int columnsMigrated = columns.size();

        // Delete source attribute
        attributeRepository.deleteById(source.getId());
        log.info("Deleted source attribute '{}' (id={})", source.getName(), source.getId());

        return MergeAttributeResult.builder()
                .survivingAttributeId(target.getId())
                .survivingAttributeName(target.getName())
                .columnsMigrated(columnsMigrated)
                .warnings(warnings)
                .build();
    }
}
