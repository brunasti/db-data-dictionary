package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.AttributeDefinitionDto;
import it.brunasti.dbdadi.exception.ResourceNotFoundException;
import it.brunasti.dbdadi.model.AttributeDefinition;
import it.brunasti.dbdadi.model.EntityDefinition;
import it.brunasti.dbdadi.repository.AttributeDefinitionRepository;
import it.brunasti.dbdadi.repository.EntityDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AttributeDefinitionService {

    private final AttributeDefinitionRepository repository;
    private final EntityDefinitionRepository entityRepository;

    public List<AttributeDefinitionDto> findAll() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
    }

    public List<AttributeDefinitionDto> findByEntity(Long entityId) {
        return repository.findByEntityIdOrderByNameAsc(entityId).stream().map(this::toDto).toList();
    }

    public AttributeDefinitionDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    @Transactional
    public AttributeDefinitionDto create(AttributeDefinitionDto dto) {
        AttributeDefinition entity = AttributeDefinition.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .entity(resolveEntity(dto.getEntityId()))
                .build();
        return toDto(repository.save(entity));
    }

    @Transactional
    public AttributeDefinitionDto update(Long id, AttributeDefinitionDto dto) {
        AttributeDefinition existing = getOrThrow(id);
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setEntity(resolveEntity(dto.getEntityId()));
        return toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        getOrThrow(id);
        repository.deleteById(id);
    }

    private EntityDefinition resolveEntity(Long entityId) {
        if (entityId == null) return null;
        return entityRepository.findById(entityId)
                .orElseThrow(() -> new ResourceNotFoundException("EntityDefinition", entityId));
    }

    private AttributeDefinition getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AttributeDefinition", id));
    }

    private AttributeDefinitionDto toDto(AttributeDefinition e) {
        return AttributeDefinitionDto.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .entityId(e.getEntity() != null ? e.getEntity().getId() : null)
                .entityName(e.getEntity() != null ? e.getEntity().getName() : null)
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
