package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.dto.AnalysisApplyRequest;
import it.brunasti.dbdadi.dto.AnalysisApplyResult;
import it.brunasti.dbdadi.dto.AnalysisResult;
import it.brunasti.dbdadi.service.AnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Tag(name = "Analysis", description = "Cross-database matching: find tables and columns that correspond across DB models")
public class AnalysisController {

    private final AnalysisService service;

    @PostMapping("/run")
    @Operation(summary = "Analyse all loaded databases and return Entity/Attribute match suggestions")
    public AnalysisResult run() {
        return service.analyze();
    }

    @PostMapping("/apply")
    @Operation(summary = "Apply selected suggestions: create/link Entities and Attributes")
    public AnalysisApplyResult apply(@RequestBody AnalysisApplyRequest request) {
        return service.apply(request);
    }
}
