package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.EntityDefinitionDto;
import it.brunasti.dbdadi.exception.ResourceNotFoundException;
import it.brunasti.dbdadi.model.EntityDefinition;
import it.brunasti.dbdadi.repository.EntityDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EntityDefinitionService {

    private final EntityDefinitionRepository repository;

    public List<EntityDefinitionDto> findAll() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toDto).toList();
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
