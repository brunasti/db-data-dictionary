package it.brunasti.dbdadi.repository;

import it.brunasti.dbdadi.model.TableDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TableDefinitionRepository extends JpaRepository<TableDefinition, Long> {

    List<TableDefinition> findBySchemaId(Long schemaId);

    List<TableDefinition> findBySchema_DatabaseModel_Id(Long databaseModelId);

    Optional<TableDefinition> findBySchemaIdAndName(Long schemaId, String name);

    boolean existsBySchemaIdAndName(Long schemaId, String name);
}
