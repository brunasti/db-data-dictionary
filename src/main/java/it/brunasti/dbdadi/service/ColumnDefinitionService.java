package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.ColumnDefinitionDto;
import it.brunasti.dbdadi.exception.DuplicateResourceException;
import it.brunasti.dbdadi.exception.ResourceNotFoundException;
import it.brunasti.dbdadi.model.ColumnDefinition;
import it.brunasti.dbdadi.model.TableDefinition;
import it.brunasti.dbdadi.repository.ColumnDefinitionRepository;
import it.brunasti.dbdadi.repository.TableDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ColumnDefinitionService {

    private final ColumnDefinitionRepository repository;
    private final TableDefinitionRepository tableRepository;

    public List<ColumnDefinitionDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public ColumnDefinitionDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    public List<ColumnDefinitionDto> findByTable(Long tableId) {
        return repository.findByTableIdOrderByOrdinalPosition(tableId).stream().map(this::toDto).toList();
    }

    public List<ColumnDefinitionDto> findBySchema(Long schemaId) {
        return repository.findByTable_Schema_Id(schemaId).stream().map(this::toDto).toList();
    }

    public List<ColumnDefinitionDto> findByDatabaseModel(Long databaseModelId) {
        return repository.findByTable_Schema_DatabaseModel_Id(databaseModelId).stream().map(this::toDto).toList();
    }

    @Transactional
    public ColumnDefinitionDto create(ColumnDefinitionDto dto) {
        if (repository.existsByTableIdAndName(dto.getTableId(), dto.getName())) {
            throw new DuplicateResourceException(
                    "Column already exists with name: " + dto.getName() + " in table id: " + dto.getTableId());
        }
        TableDefinition table = tableRepository.findById(dto.getTableId())
                .orElseThrow(() -> new ResourceNotFoundException("TableDefinition", dto.getTableId()));
        ColumnDefinition entity = toEntity(dto, table);
        return toDto(repository.save(entity));
    }

    @Transactional
    public ColumnDefinitionDto update(Long id, ColumnDefinitionDto dto) {
        ColumnDefinition existing = getOrThrow(id);
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setDataType(dto.getDataType());
        existing.setLength(dto.getLength());
        existing.setPrecision(dto.getPrecision());
        existing.setScale(dto.getScale());
        existing.setNullable(dto.isNullable());
        existing.setPrimaryKey(dto.isPrimaryKey());
        existing.setUnique(dto.isUnique());
        existing.setDefaultValue(dto.getDefaultValue());
        existing.setOrdinalPosition(dto.getOrdinalPosition());
        return toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        getOrThrow(id);
        repository.deleteById(id);
    }

    private ColumnDefinition getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("ColumnDefinition", id));
    }

    private ColumnDefinitionDto toDto(ColumnDefinition e) {
        return ColumnDefinitionDto.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .dataType(e.getDataType())
                .length(e.getLength())
                .precision(e.getPrecision())
                .scale(e.getScale())
                .nullable(e.isNullable())
                .primaryKey(e.isPrimaryKey())
                .unique(e.isUnique())
                .defaultValue(e.getDefaultValue())
                .ordinalPosition(e.getOrdinalPosition())
                .tableId(e.getTable().getId())
                .tableName(e.getTable().getName())
                .schemaId(e.getTable().getSchema().getId())
                .schemaName(e.getTable().getSchema().getName())
                .databaseModelId(e.getTable().getSchema().getDatabaseModel().getId())
                .databaseModelName(e.getTable().getSchema().getDatabaseModel().getName())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private ColumnDefinition toEntity(ColumnDefinitionDto dto, TableDefinition table) {
        return ColumnDefinition.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .dataType(dto.getDataType())
                .length(dto.getLength())
                .precision(dto.getPrecision())
                .scale(dto.getScale())
                .nullable(dto.isNullable())
                .primaryKey(dto.isPrimaryKey())
                .unique(dto.isUnique())
                .defaultValue(dto.getDefaultValue())
                .ordinalPosition(dto.getOrdinalPosition())
                .table(table)
                .build();
    }
}
