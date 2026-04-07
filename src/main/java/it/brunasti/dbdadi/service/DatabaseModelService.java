package it.brunasti.dbdadi.service;

import it.brunasti.dbdadi.dto.DatabaseModelDto;
import it.brunasti.dbdadi.exception.DuplicateResourceException;
import it.brunasti.dbdadi.exception.ResourceNotFoundException;
import it.brunasti.dbdadi.model.DatabaseModel;
import it.brunasti.dbdadi.model.enums.DbType;
import it.brunasti.dbdadi.repository.DatabaseModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DatabaseModelService {

    private final DatabaseModelRepository repository;

    public List<DatabaseModelDto> findAll() {
        return repository.findAll().stream().map(this::toDto).toList();
    }

    public DatabaseModelDto findById(Long id) {
        return toDto(getOrThrow(id));
    }

    public List<DatabaseModelDto> findByDbType(DbType dbType) {
        return repository.findByDbType(dbType).stream().map(this::toDto).toList();
    }

    @Transactional
    public DatabaseModelDto create(DatabaseModelDto dto) {
        if (repository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("DatabaseModel already exists with name: " + dto.getName());
        }
        DatabaseModel entity = toEntity(dto);
        return toDto(repository.save(entity));
    }

    @Transactional
    public DatabaseModelDto update(Long id, DatabaseModelDto dto) {
        DatabaseModel existing = getOrThrow(id);
        if (!existing.getName().equals(dto.getName()) && repository.existsByName(dto.getName())) {
            throw new DuplicateResourceException("DatabaseModel already exists with name: " + dto.getName());
        }
        existing.setName(dto.getName());
        existing.setDescription(dto.getDescription());
        existing.setDbType(dto.getDbType());
        existing.setVersion(dto.getVersion());
        return toDto(repository.save(existing));
    }

    @Transactional
    public void delete(Long id) {
        getOrThrow(id);
        repository.deleteById(id);
    }

    private DatabaseModel getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("DatabaseModel", id));
    }

    private DatabaseModelDto toDto(DatabaseModel e) {
        return DatabaseModelDto.builder()
                .id(e.getId())
                .name(e.getName())
                .description(e.getDescription())
                .dbType(e.getDbType())
                .version(e.getVersion())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private DatabaseModel toEntity(DatabaseModelDto dto) {
        return DatabaseModel.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .dbType(dto.getDbType())
                .version(dto.getVersion())
                .build();
    }
}
