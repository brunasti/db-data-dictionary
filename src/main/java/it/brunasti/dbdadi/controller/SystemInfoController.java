package it.brunasti.dbdadi.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.brunasti.dbdadi.aspect.Loggable;
import it.brunasti.dbdadi.dto.SystemInfoDto;
import it.brunasti.dbdadi.service.SystemInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/system-info")
@RequiredArgsConstructor
@Tag(name = "System Info", description = "Runtime system information")
public class SystemInfoController {

    private final SystemInfoService service;

    @GetMapping
    @Operation(summary = "Get application runtime information")
    @Loggable
    public SystemInfoDto get() {
        return service.getInfo();
    }
}
