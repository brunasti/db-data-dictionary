package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.DomainDefinitionDto;
import it.brunasti.dbdadi.dto.EntityDefinitionDto;
import it.brunasti.dbdadi.exception.ResourceNotFoundException;
import it.brunasti.dbdadi.model.DomainDefinition;
import it.brunasti.dbdadi.model.EntityDefinition;
import it.brunasti.dbdadi.repository.DomainDefinitionRepository;
import it.brunasti.dbdadi.repository.EntityDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntityDefinitionService {

    private final EntityDefinitionRepository repository;
    private final DomainDefinitionRepository domainRepository;

    public List<EntityDefinitionDto> findAll() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    public List<EntityDefinitionDto> findByDomain(Long domainId) {
        return repository.findByDomains_Id(domainId).stream()
                .sorted(Comparator.comparing(EntityDefinition::getName))
                .map(this::toDto)
                .toList();
    }

    public EntityDefinitionDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    @Transactional
    public EntityDefinitionDto create(EntityDefinitionDto dto) {
        EntityDefinition entity = EntityDefinition.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
        return toDto(repository.save(entity));
    }

    @Transactional
    public EntityDefinitionDto update(Long id, EntityDefinitionDto dto) {
        EntityDefinition existing = getOrThrow(id);
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        return toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        getOrThrow(id);
        repository.deleteById(id);
    }

    public List<DomainDefinitionDto> findDomains(Long entityId) {
        return domainRepository.findByEntities_Id(entityId).stream()
                .sorted(Comparator.comparing(DomainDefinition::getName))
                .map(d -> DomainDefinitionDto.builder()
                        .id(d.getId())
                        .name(d.getName())
                        .description(d.getDescription())
                        .createdAt(d.getCreatedAt())
                        .updatedAt(d.getUpdatedAt())
                        .build())
                .toList();
    }

    @Transactional
    public void setDomains(Long entityId, List<Long> domainIds) {
        EntityDefinition entity = getOrThrow(entityId);

        List<DomainDefinition> currentDomains = domainRepository.findByEntities_Id(entityId);
        Set<Long> requestedIds = domainIds == null ? Set.of() : Set.copyOf(domainIds);

        for (DomainDefinition domain : currentDomains) {
            if (!requestedIds.contains(domain.getId())) {
                domain.getEntities().remove(entity);
                domainRepository.save(domain);
            }
        }

        Set<Long> currentIds = currentDomains.stream().map(DomainDefinition::getId).collect(Collectors.toSet());
        for (Long domainId : requestedIds) {
            if (!currentIds.contains(domainId)) {
                DomainDefinition domain = domainRepository.findById(domainId)
                        .orElseThrow(() -> new ResourceNotFoundException("DomainDefinition", domainId));
                domain.getEntities().add(entity);
                domainRepository.save(domain);
            }
        }
    }

    private EntityDefinition getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("EntityDefinition", id));
    }

    private EntityDefinitionDto toDto(EntityDefinition e) {
        return EntityDefinitionDto.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
