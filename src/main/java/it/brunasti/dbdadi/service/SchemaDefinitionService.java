package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.SchemaDefinitionDto;
import it.brunasti.dbdadi.exception.DuplicateResourceException;
import it.brunasti.dbdadi.exception.ResourceNotFoundException;
import it.brunasti.dbdadi.model.DatabaseModel;
import it.brunasti.dbdadi.model.SchemaDefinition;
import it.brunasti.dbdadi.repository.DatabaseModelRepository;
import it.brunasti.dbdadi.repository.SchemaDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SchemaDefinitionService {

    private final SchemaDefinitionRepository repository;
    private final DatabaseModelRepository databaseModelRepository;

    public List<SchemaDefinitionDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public SchemaDefinitionDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    public List<SchemaDefinitionDto> findByDatabaseModel(Long databaseModelId) {
        return repository.findByDatabaseModelId(databaseModelId).stream().map(this::toDto).toList();
    }

    @Transactional
    public SchemaDefinitionDto create(SchemaDefinitionDto dto) {
        if (repository.existsByDatabaseModelIdAndName(dto.getDatabaseModelId(), dto.getName())) {
            throw new DuplicateResourceException(
                    "Schema already exists with name: " + dto.getName() + " in database model id: " + dto.getDatabaseModelId());
        }
        DatabaseModel dbModel = databaseModelRepository.findById(dto.getDatabaseModelId())
                .orElseThrow(() -> new ResourceNotFoundException("DatabaseModel", dto.getDatabaseModelId()));
        return toDto(repository.save(toEntity(dto, dbModel)));
    }

    @Transactional
    public SchemaDefinitionDto update(Long id, SchemaDefinitionDto dto) {
        SchemaDefinition existing = getOrThrow(id);
        if (!existing.getName().equals(dto.getName()) &&
                repository.existsByDatabaseModelIdAndName(existing.getDatabaseModel().getId(), dto.getName())) {
            throw new DuplicateResourceException(
                    "Schema already exists with name: " + dto.getName());
        }
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        return toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        getOrThrow(id);
        repository.deleteById(id);
    }

    private SchemaDefinition getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("SchemaDefinition", id));
    }

    private SchemaDefinitionDto toDto(SchemaDefinition e) {
        return SchemaDefinitionDto.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .databaseModelId(e.getDatabaseModel().getId())
                .databaseModelName(e.getDatabaseModel().getName())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private SchemaDefinition toEntity(SchemaDefinitionDto dto, DatabaseModel dbModel) {
        return SchemaDefinition.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .databaseModel(dbModel)
                .build();
    }
}
