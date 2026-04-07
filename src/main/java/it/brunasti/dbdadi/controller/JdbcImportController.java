package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.dto.JdbcImportRequest;
import it.brunasti.dbdadi.dto.JdbcImportResult;
import it.brunasti.dbdadi.service.JdbcImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/import")
@RequiredArgsConstructor
@Tag(name = "Import", description = "Import data dictionary from an existing database via JDBC")
public class JdbcImportController {

    private final JdbcImportService service;

    @PostMapping("/jdbc")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Import from JDBC",
            description = """
                    Connects to any JDBC-compatible database and imports schemas, tables,
                    columns and foreign-key relationships into the data dictionary.

                    Supported databases (driver must be on classpath):
                    - PostgreSQL (included)
                    - MySQL (included)
                    - H2 (included)
                    - Oracle (add ojdbc11 to pom.xml)
                    - DB2 (add db2jcc4 to pom.xml)
                    """
    )
    public JdbcImportResult importFromJdbc(@Valid @RequestBody JdbcImportRequest request) {
        return service.importFromJdbc(request);
    }
}
