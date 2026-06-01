package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.MergeEntityRequest;
import it.brunasti.dbdadi.dto.MergeEntityResult;
import it.brunasti.dbdadi.exception.ResourceNotFoundException;
import it.brunasti.dbdadi.model.AttributeDefinition;
import it.brunasti.dbdadi.model.DomainDefinition;
import it.brunasti.dbdadi.model.EntityDefinition;
import it.brunasti.dbdadi.model.TableDefinition;
import it.brunasti.dbdadi.repository.AttributeDefinitionRepository;
import it.brunasti.dbdadi.repository.DomainDefinitionRepository;
import it.brunasti.dbdadi.repository.EntityDefinitionRepository;
import it.brunasti.dbdadi.repository.TableDefinitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MergeEntityService {

    private final EntityDefinitionRepository entityRepository;
    private final AttributeDefinitionRepository attributeRepository;
    private final TableDefinitionRepository tableRepository;
    private final DomainDefinitionRepository domainRepository;

    @Transactional
    public MergeEntityResult merge(MergeEntityRequest request) {
        if (request.getSourceEntityId() == null || request.getTargetEntityId() == null) {
            throw new IllegalArgumentException("Both sourceEntityId and targetEntityId are required");
        }
        if (request.getSourceEntityId().equals(request.getTargetEntityId())) {
            throw new IllegalArgumentException("Source and target entity must be different");
        }

        EntityDefinition source = entityRepository.findById(request.getSourceEntityId())
                .orElseThrow(() -> new ResourceNotFoundException("EntityDefinition", request.getSourceEntityId()));
        EntityDefinition target = entityRepository.findById(request.getTargetEntityId())
                .orElseThrow(() -> new ResourceNotFoundException("EntityDefinition", request.getTargetEntityId()));

        List<String> warnings = new java.util.ArrayList<>();

        // 1. Migrate attributes
        List<AttributeDefinition> sourceAttributes =
                attributeRepository.findByEntityIdOrderByNameAsc(source.getId());
        int attributesMigrated = 0;
        for (AttributeDefinition attr : sourceAttributes) {
            // Check if target already has an attribute with the same name
            boolean collision = attributeRepository
                    .findByNameIgnoreCaseAndEntityId(attr.getName(), target.getId())
                    .isPresent();
            if (collision) {
                warnings.add("Attribute '" + attr.getName()
                        + "' already exists on target entity — kept as-is on source (manual review needed).");
            } else {
                attr.setEntity(target);
                attributeRepository.save(attr);
                attributesMigrated++;
                log.info("Migrated attribute '{}' from entity '{}' to '{}'",
                        attr.getName(), source.getName(), target.getName());
            }
        }

        // 2. Migrate table links
        List<TableDefinition> sourceTables = tableRepository.findByEntityId(source.getId());
        for (TableDefinition table : sourceTables) {
            table.setEntity(target);
            tableRepository.save(table);
            log.info("Re-linked table '{}' from entity '{}' to '{}'",
                    table.getName(), source.getName(), target.getName());
        }
        int tablesMigrated = sourceTables.size();

        // 3. Migrate domain memberships
        List<DomainDefinition> sourceDomains = domainRepository.findByEntities_Id(source.getId());
        int domainsMigrated = 0;
        for (DomainDefinition domain : sourceDomains) {
            boolean alreadyLinked = domain.getEntities().stream()
                    .anyMatch(e -> e.getId().equals(target.getId()));
            if (!alreadyLinked) {
                domain.getEntities().add(target);
                domainRepository.save(domain);
                domainsMigrated++;
                log.info("Added entity '{}' to domain '{}'", target.getName(), domain.getName());
            }
            // Remove source from the domain
            domain.getEntities().removeIf(e -> e.getId().equals(source.getId()));
            domainRepository.save(domain);
        }

        // 4. Delete source entity (only if no attributes remain linked to it)
        long remainingAttrs = attributeRepository.findByEntityIdOrderByNameAsc(source.getId()).size();
        if (remainingAttrs == 0) {
            entityRepository.deleteById(source.getId());
            log.info("Deleted source entity '{}'", source.getName());
        } else {
            warnings.add("Source entity '" + source.getName()
                    + "' was NOT deleted because " + remainingAttrs
                    + " attribute(s) still reference it (name conflicts). Resolve them manually.");
        }

        return MergeEntityResult.builder()
                .survivingEntityId(target.getId())
                .survivingEntityName(target.getName())
                .attributesMigrated(attributesMigrated)
                .tablesMigrated(tablesMigrated)
                .domainsMigrated(domainsMigrated)
                .warnings(warnings)
                .build();
    }
}
