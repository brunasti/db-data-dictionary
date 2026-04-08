package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.RelationshipDefinitionDto;
import it.brunasti.dbdadi.exception.ResourceNotFoundException;
import it.brunasti.dbdadi.model.RelationshipDefinition;
import it.brunasti.dbdadi.model.TableDefinition;
import it.brunasti.dbdadi.repository.RelationshipDefinitionRepository;
import it.brunasti.dbdadi.repository.TableDefinitionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RelationshipDefinitionService {

    private final RelationshipDefinitionRepository repository;
    private final TableDefinitionRepository tableRepository;

    public List<RelationshipDefinitionDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public RelationshipDefinitionDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    public List<RelationshipDefinitionDto> findByFromTable(Long fromTableId) {
        return repository.findByFromTableId(fromTableId).stream().map(this::toDto).toList();
    }

    public List<RelationshipDefinitionDto> findByToTable(Long toTableId) {
        return repository.findByToTableId(toTableId).stream().map(this::toDto).toList();
    }

    public List<RelationshipDefinitionDto> findByDatabaseModel(Long databaseModelId) {
        return repository.findByFromTable_Schema_DatabaseModel_Id(databaseModelId).stream().map(this::toDto).toList();
    }

    @Transactional
    public RelationshipDefinitionDto create(RelationshipDefinitionDto dto) {
        TableDefinition fromTable = tableRepository.findById(dto.getFromTableId())
                .orElseThrow(() -> new ResourceNotFoundException("TableDefinition", dto.getFromTableId()));
        TableDefinition toTable = tableRepository.findById(dto.getToTableId())
                .orElseThrow(() -> new ResourceNotFoundException("TableDefinition", dto.getToTableId()));
        RelationshipDefinition entity = toEntity(dto, fromTable, toTable);
        return toDto(repository.save(entity));
    }

    @Transactional
    public RelationshipDefinitionDto update(Long id, RelationshipDefinitionDto dto) {
        RelationshipDefinition existing = getOrThrow(id);
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setType(dto.getType());
        existing.setFromColumnName(dto.getFromColumnName());
        existing.setToColumnName(dto.getToColumnName());
        return toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        getOrThrow(id);
        repository.deleteById(id);
    }

    private RelationshipDefinition getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("RelationshipDefinition", id));
    }

    private RelationshipDefinitionDto toDto(RelationshipDefinition e) {
        return RelationshipDefinitionDto.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .type(e.getType())
                .fromTableId(e.getFromTable().getId())
                .fromTableName(e.getFromTable().getName())
                .fromColumnName(e.getFromColumnName())
                .toTableId(e.getToTable().getId())
                .toTableName(e.getToTable().getName())
                .toColumnName(e.getToColumnName())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private RelationshipDefinition toEntity(RelationshipDefinitionDto dto,
                                             TableDefinition fromTable, TableDefinition toTable) {
        return RelationshipDefinition.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .type(dto.getType())
                .fromTable(fromTable)
                .fromColumnName(dto.getFromColumnName())
                .toTable(toTable)
                .toColumnName(dto.getToColumnName())
                .build();
    }
}
