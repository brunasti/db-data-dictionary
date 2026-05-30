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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DomainDefinitionService {

    private final DomainDefinitionRepository repository;
    private final EntityDefinitionRepository entityRepository;

    public List<DomainDefinitionDto> findAll() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    public DomainDefinitionDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    public List<DomainDefinitionDto> findByEntity(Long entityId) {
        return repository.findByEntities_Id(entityId).stream()
                .sorted(Comparator.comparing(DomainDefinition::getName))
                .map(this::toDto)
                .toList();
    }

    public List<EntityDefinitionDto> findEntities(Long domainId) {
        return entityRepository.findByDomains_Id(domainId).stream()
                .sorted(Comparator.comparing(EntityDefinition::getName))
                .map(this::toEntityDto)
                .toList();
    }

    @Transactional
    public DomainDefinitionDto create(DomainDefinitionDto dto) {
        DomainDefinition domain = DomainDefinition.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .build();
        return toDto(repository.save(domain));
    }

    @Transactional
    public DomainDefinitionDto update(Long id, DomainDefinitionDto dto) {
        DomainDefinition existing = getOrThrow(id);
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        return toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        getOrThrow(id);
        repository.deleteById(id);
    }

    @Transactional
    public void setEntities(Long domainId, List<Long> entityIds) {
        DomainDefinition domain = getOrThrow(domainId);
        List<EntityDefinition> newEntities = entityIds == null || entityIds.isEmpty()
                ? List.of()
                : entityIds.stream()
                        .map(eid -> entityRepository.findById(eid)
                                .orElseThrow(() -> new ResourceNotFoundException("EntityDefinition", eid)))
                        .toList();
        domain.getEntities().clear();
        domain.getEntities().addAll(newEntities);
        repository.save(domain);
    }

    private DomainDefinition getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DomainDefinition", id));
    }

    private DomainDefinitionDto toDto(DomainDefinition d) {
        return DomainDefinitionDto.builder()
                .id(d.getId())
                .name(d.getName())
                .description(d.getDescription())
                .createdAt(d.getCreatedAt())
                .updatedAt(d.getUpdatedAt())
                .build();
    }

    private EntityDefinitionDto toEntityDto(EntityDefinition e) {
        return EntityDefinitionDto.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
