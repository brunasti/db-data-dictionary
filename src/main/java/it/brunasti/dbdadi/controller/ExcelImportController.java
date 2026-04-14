package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.dto.ExcelImportResult;
import it.brunasti.dbdadi.service.ExcelImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/admin/import")
@RequiredArgsConstructor
@Tag(name = "Admin - Import", description = "Import data dictionary from Excel")
public class ExcelImportController {

    private final ExcelImportService service;

    @PostMapping(value = "/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Import full data dictionary from an Excel (.xlsx) file")
    public ExcelImportResult importExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "clearBeforeImport", defaultValue = "false") boolean clearBeforeImport)
            throws IOException {
        return service.importFromExcel(file.getBytes(), clearBeforeImport);
    }
}
