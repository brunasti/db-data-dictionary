package it.brunasti.dbdadi.repository;

import it.brunasti.dbdadi.model.Association;
import it.brunasti.dbdadi.model.enums.RelationshipType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssociationRepository extends JpaRepository<Association, Long> {

    List<Association> findAllByOrderByNameAsc();

    List<Association> findByFromEntityIdOrderByNameAsc(Long fromEntityId);

    List<Association> findByToEntityIdOrderByNameAsc(Long toEntityId);

    Optional<Association> findByFromEntityIdAndToEntityIdAndType(Long fromEntityId, Long toEntityId, RelationshipType type);
}
