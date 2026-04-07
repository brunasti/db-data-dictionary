package it.brunasti.dbdadi.repository;

import it.brunasti.dbdadi.model.RelationshipDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RelationshipDefinitionRepository extends JpaRepository<RelationshipDefinition, Long> {

    List<RelationshipDefinition> findByFromTableId(Long fromTableId);

    List<RelationshipDefinition> findByToTableId(Long toTableId);

    List<RelationshipDefinition> findByFromTableDatabaseModelId(Long databaseModelId);
}
