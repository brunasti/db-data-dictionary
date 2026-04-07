package it.brunasti.dbdadi.repository;

import it.brunasti.dbdadi.model.DatabaseModel;
import it.brunasti.dbdadi.model.enums.DbType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DatabaseModelRepository extends JpaRepository<DatabaseModel, Long> {

    Optional<DatabaseModel> findByName(String name);

    List<DatabaseModel> findByDbType(DbType dbType);

    boolean existsByName(String name);
}
