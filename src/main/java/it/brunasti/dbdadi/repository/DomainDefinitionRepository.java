package it.brunasti.dbdadi.repository;

import it.brunasti.dbdadi.model.DomainDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DomainDefinitionRepository extends JpaRepository<DomainDefinition, Long> {

    List<DomainDefinition> findAllByOrderByNameAsc();

    List<DomainDefinition> findByEntities_Id(Long entityId);
}
