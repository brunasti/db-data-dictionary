package it.brunasti.dbdadi.repository;

import it.brunasti.dbdadi.model.TableDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TableDefinitionRepository extends JpaRepository<TableDefinition, Long> {

    List<TableDefinition> findByDatabaseModelId(Long databaseModelId);

    Optional<TableDefinition> findByDatabaseModelIdAndSchemaNameAndName(Long databaseModelId, String schemaName, String name);

    boolean existsByDatabaseModelIdAndSchemaNameAndName(Long databaseModelId, String schemaName, String name);
}
