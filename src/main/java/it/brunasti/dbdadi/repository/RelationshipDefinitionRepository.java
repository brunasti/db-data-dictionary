package it.brunasti.dbdadi.repository;

import it.brunasti.dbdadi.model.RelationshipDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RelationshipDefinitionRepository extends JpaRepository<RelationshipDefinition, Long> {

    List<RelationshipDefinition> findByFromTableId(Long fromTableId);

    List<RelationshipDefinition> findByFromTable_Entity_Id(Long entityId);

    List<RelationshipDefinition> findByToTableId(Long toTableId);

    List<RelationshipDefinition> findByFromTable_Schema_DatabaseModel_Id(Long databaseModelId);

    List<RelationshipDefinition> findByFromTable_Schema_Id(Long schemaId);

    List<RelationshipDefinition> findByToTable_Schema_Id(Long schemaId);
}
