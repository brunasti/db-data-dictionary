package it.brunasti.dbdadi.repository;

import it.brunasti.dbdadi.model.ColumnDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ColumnDefinitionRepository extends JpaRepository<ColumnDefinition, Long> {

    List<ColumnDefinition> findByTableIdOrderByOrdinalPosition(Long tableId);

    Optional<ColumnDefinition> findByTableIdAndName(Long tableId, String name);

    boolean existsByTableIdAndName(Long tableId, String name);
}
