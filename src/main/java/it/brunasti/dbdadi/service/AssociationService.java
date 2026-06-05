package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.AssociationDto;
import it.brunasti.dbdadi.dto.GenerateAssociationsResult;
import it.brunasti.dbdadi.exception.ResourceNotFoundException;
import it.brunasti.dbdadi.model.Association;
import it.brunasti.dbdadi.model.EntityDefinition;
import it.brunasti.dbdadi.model.RelationshipDefinition;
import it.brunasti.dbdadi.repository.AssociationRepository;
import it.brunasti.dbdadi.repository.EntityDefinitionRepository;
import it.brunasti.dbdadi.repository.RelationshipDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssociationService {

    private final AssociationRepository repository;
    private final EntityDefinitionRepository entityRepository;
    private final RelationshipDefinitionRepository relationshipRepository;

    public List<AssociationDto> findAll() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    public List<AssociationDto> findByEntity(Long entityId) {
        Set<Long> seen = new LinkedHashSet<>();
        List<AssociationDto> result = new ArrayList<>();
        repository.findByFromEntityIdOrderByNameAsc(entityId).forEach(a -> {
            seen.add(a.getId());
            result.add(toDto(a));
        });
        repository.findByToEntityIdOrderByNameAsc(entityId).forEach(a -> {
            if (!seen.contains(a.getId())) result.add(toDto(a));
        });
        result.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return result;
    }

    public AssociationDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    @Transactional
    public AssociationDto create(AssociationDto dto) {
        Association association = Association.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .type(dto.getType())
                .fromEntity(resolveEntity(dto.getFromEntityId()))
                .toEntity(resolveEntity(dto.getToEntityId()))
                .build();
        return toDto(repository.save(association));
    }

    @Transactional
    public AssociationDto update(Long id, AssociationDto dto) {
        Association existing = getOrThrow(id);
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setType(dto.getType());
        existing.setFromEntity(resolveEntity(dto.getFromEntityId()));
        existing.setToEntity(resolveEntity(dto.getToEntityId()));
        return toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        getOrThrow(id);
        repository.deleteById(id);
    }

    @Transactional
    public GenerateAssociationsResult generateFromDomain(Long domainId) {
        List<EntityDefinition> domainEntities = entityRepository.findByDomains_Id(domainId);
        Set<Long> domainEntityIds = domainEntities.stream()
                .map(EntityDefinition::getId)
                .collect(Collectors.toSet());

        int created = 0, skipped = 0;
        List<String> createdNames = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        for (EntityDefinition fromEntity : domainEntities) {
            List<RelationshipDefinition> rels = relationshipRepository.findByFromTable_Entity_Id(fromEntity.getId());
            for (RelationshipDefinition rel : rels) {
                EntityDefinition toEntity = rel.getToTable().getEntity();
                if (toEntity == null) continue;
                if (!domainEntityIds.contains(toEntity.getId())) continue;

                if (repository.findByFromEntityIdAndToEntityIdAndType(
                        fromEntity.getId(), toEntity.getId(), rel.getType()).isPresent()) {
                    skipped++;
                    continue;
                }

                String name = fromEntity.getName() + "-" + toEntity.getName();
                repository.save(Association.builder()
                        .name(name)
                        .type(rel.getType())
                        .fromEntity(fromEntity)
                        .toEntity(toEntity)
                        .build());
                createdNames.add(name + " (" + rel.getType().name() + ")");
                created++;
            }
        }

        return GenerateAssociationsResult.builder()
                .associationsCreated(created)
                .associationsSkipped(skipped)
                .createdNames(createdNames)
                .warnings(warnings)
                .build();
    }

    private EntityDefinition resolveEntity(Long entityId) {
        if (entityId == null) return null;
        return entityRepository.findById(entityId)
                .orElseThrow(() -> new ResourceNotFoundException("EntityDefinition", entityId));
    }

    private Association getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Association", id));
    }

    private AssociationDto toDto(Association a) {
        return AssociationDto.builder()
                .id(a.getId())
                .name(a.getName())
                .description(a.getDescription())
                .type(a.getType())
                .fromEntityId(a.getFromEntity() != null ? a.getFromEntity().getId() : null)
                .fromEntityName(a.getFromEntity() != null ? a.getFromEntity().getName() : null)
                .toEntityId(a.getToEntity() != null ? a.getToEntity().getId() : null)
                .toEntityName(a.getToEntity() != null ? a.getToEntity().getName() : null)
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
