package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.TableDefinitionDto;
import it.brunasti.dbdadi.exception.DuplicateResourceException;
import it.brunasti.dbdadi.exception.ResourceNotFoundException;
import it.brunasti.dbdadi.model.DatabaseModel;
import it.brunasti.dbdadi.model.TableDefinition;
import it.brunasti.dbdadi.repository.DatabaseModelRepository;
import it.brunasti.dbdadi.repository.TableDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TableDefinitionService {

    private final TableDefinitionRepository repository;
    private final DatabaseModelRepository databaseModelRepository;

    public List<TableDefinitionDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public TableDefinitionDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    public List<TableDefinitionDto> findByDatabaseModel(Long databaseModelId) {
        return repository.findByDatabaseModelId(databaseModelId).stream().map(this::toDto).toList();
    }

    @Transactional
    public TableDefinitionDto create(TableDefinitionDto dto) {
        if (repository.existsByDatabaseModelIdAndSchemaNameAndName(
                dto.getDatabaseModelId(), dto.getSchemaName(), dto.getName())) {
            throw new DuplicateResourceException(
                    "Table already exists with name: " + dto.getName() + " in schema: " + dto.getSchemaName());
        }
        DatabaseModel dbModel = databaseModelRepository.findById(dto.getDatabaseModelId())
                .orElseThrow(() -> new ResourceNotFoundException("DatabaseModel", dto.getDatabaseModelId()));
        TableDefinition entity = toEntity(dto, dbModel);
        return toDto(repository.save(entity));
    }

    @Transactional
    public TableDefinitionDto update(Long id, TableDefinitionDto dto) {
        TableDefinition existing = getOrThrow(id);
        existing.setName(dto.getName());
        existing.setSchemaName(dto.getSchemaName());
        existing.setDescription(dto.getDescription());
        return toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        getOrThrow(id);
        repository.deleteById(id);
    }

    private TableDefinition getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("TableDefinition", id));
    }

    private TableDefinitionDto toDto(TableDefinition e) {
        return TableDefinitionDto.builder()
                .id(e.getId())
                .name(e.getName())
                .schemaName(e.getSchemaName())
                .description(e.getDescription())
                .databaseModelId(e.getDatabaseModel().getId())
                .databaseModelName(e.getDatabaseModel().getName())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private TableDefinition toEntity(TableDefinitionDto dto, DatabaseModel dbModel) {
        return TableDefinition.builder()
                .name(dto.getName())
                .schemaName(dto.getSchemaName())
                .description(dto.getDescription())
                .databaseModel(dbModel)
                .build();
    }
}
