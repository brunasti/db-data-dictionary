package it.brunasti.dbdadi.repository;

import it.brunasti.dbdadi.model.Association;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssociationRepository extends JpaRepository<Association, Long> {

    List<Association> findAllByOrderByNameAsc();

    List<Association> findByFromEntityIdOrderByNameAsc(Long fromEntityId);

    List<Association> findByToEntityIdOrderByNameAsc(Long toEntityId);
}
