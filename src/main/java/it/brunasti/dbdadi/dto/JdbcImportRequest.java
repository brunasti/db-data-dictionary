package it.brunasti.dbdadi.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JdbcImportRequest {

    @NotBlank
    private String modelName;       // name for the DatabaseModel entry in dbdadi

    @NotBlank
    private String jdbcUrl;         // e.g. jdbc:postgresql://localhost:5432/mydb

    private String username;

    private String password;

    private String schemaPattern;   // null/empty = all non-system schemas; supports SQL % wildcard

    private String tablePattern;    // null/empty = all tables; supports SQL % wildcard

    private boolean includeViews;   // also import VIEWs (default: false)

    private boolean overwrite;      // delete existing model with same name before importing
}
