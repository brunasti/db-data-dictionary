package it.brunasti.dbdadi.repository;

import it.brunasti.dbdadi.model.EntityDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EntityDefinitionRepository extends JpaRepository<EntityDefinition, Long> {

    List<EntityDefinition> findAllByOrderByNameAsc();

    List<EntityDefinition> findByDomains_Id(Long domainId);

    java.util.Optional<EntityDefinition> findByNameIgnoreCase(String name);
}
