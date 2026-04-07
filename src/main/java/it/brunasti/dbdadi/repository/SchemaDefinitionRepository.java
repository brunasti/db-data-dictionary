package it.brunasti.dbdadi.repository;

import it.brunasti.dbdadi.model.SchemaDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SchemaDefinitionRepository extends JpaRepository<SchemaDefinition, Long> {

    List<SchemaDefinition> findByDatabaseModelId(Long databaseModelId);

    Optional<SchemaDefinition> findByDatabaseModelIdAndName(Long databaseModelId, String name);

    boolean existsByDatabaseModelIdAndName(Long databaseModelId, String name);
}
