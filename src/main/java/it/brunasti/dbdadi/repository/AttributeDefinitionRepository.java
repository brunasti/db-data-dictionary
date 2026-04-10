package it.brunasti.dbdadi.repository;

import it.brunasti.dbdadi.model.AttributeDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, Long> {

    List<AttributeDefinition> findAllByOrderByNameAsc();
}
