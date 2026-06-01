package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.aspect.Loggable;
import it.brunasti.dbdadi.dto.AlignmentRequest;
import it.brunasti.dbdadi.dto.AlignmentResult;
import it.brunasti.dbdadi.service.AlignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/alignment")
@RequiredArgsConstructor
@Tag(name = "Alignment", description = "Check alignment between a stored Database Model and its JDBC source")
public class AlignmentController {

    private final AlignmentService service;

    @PostMapping
    @Operation(summary = "Compare a stored DatabaseModel against its live JDBC source and return a diff report")
    @Loggable
    public AlignmentResult check(@RequestBody AlignmentRequest request) {
        return service.check(request);
    }
}
