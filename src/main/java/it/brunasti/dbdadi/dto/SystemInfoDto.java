package it.brunasti.dbdadi.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemInfoDto {

    private String appVersion;
    private LocalDateTime startupTime;
    private LocalDateTime serverTime;
    private long uptimeSeconds;
    private String javaVersion;
    private String javaVendor;
    private String osName;
    private String osVersion;
}
