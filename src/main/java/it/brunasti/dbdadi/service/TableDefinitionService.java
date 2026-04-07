package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.TableDefinitionDto;
import it.brunasti.dbdadi.exception.DuplicateResourceException;
import it.brunasti.dbdadi.exception.ResourceNotFoundException;
import it.brunasti.dbdadi.model.SchemaDefinition;
import it.brunasti.dbdadi.model.TableDefinition;
import it.brunasti.dbdadi.repository.SchemaDefinitionRepository;
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
    private final SchemaDefinitionRepository schemaRepository;

    public List<TableDefinitionDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public TableDefinitionDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    public List<TableDefinitionDto> findBySchema(Long schemaId) {
        return repository.findBySchemaId(schemaId).stream().map(this::toDto).toList();
    }

    public List<TableDefinitionDto> findByDatabaseModel(Long databaseModelId) {
        return repository.findBySchema_DatabaseModel_Id(databaseModelId).stream().map(this::toDto).toList();
    }

    @Transactional
    public TableDefinitionDto create(TableDefinitionDto dto) {
        if (repository.existsBySchemaIdAndName(dto.getSchemaId(), dto.getName())) {
            throw new DuplicateResourceException(
                    "Table already exists with name: " + dto.getName() + " in schema id: " + dto.getSchemaId());
        }
        SchemaDefinition schema = schemaRepository.findById(dto.getSchemaId())
                .orElseThrow(() -> new ResourceNotFoundException("SchemaDefinition", dto.getSchemaId()));
        return toDto(repository.save(toEntity(dto, schema)));
    }

    @Transactional
    public TableDefinitionDto update(Long id, TableDefinitionDto dto) {
        TableDefinition existing = getOrThrow(id);
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        if (!existing.getSchema().getId().equals(dto.getSchemaId())) {
            SchemaDefinition schema = schemaRepository.findById(dto.getSchemaId())
                    .orElseThrow(() -> new ResourceNotFoundException("SchemaDefinition", dto.getSchemaId()));
            existing.setSchema(schema);
        }
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
                .description(e.getDescription())
                .schemaId(e.getSchema().getId())
                .schemaName(e.getSchema().getName())
                .databaseModelId(e.getSchema().getDatabaseModel().getId())
                .databaseModelName(e.getSchema().getDatabaseModel().getName())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private TableDefinition toEntity(TableDefinitionDto dto, SchemaDefinition schema) {
        return TableDefinition.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .schema(schema)
                .build();
    }
}
